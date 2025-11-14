package image.server.image_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import image.server.image_server.config.LlmProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Doubao / Volcengine provider implementation.
 *  - Uses RestTemplate (configured via RestTemplateBuilder) -> no WebFlux needed
 *  - Robust JSON parsing via Jackson ObjectMapper
 *  - Avoids raw-types and unchecked casts
 */
@Component("doubaoProvider")
public class DoubaoProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(DoubaoProvider.class);

    private final LlmProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String endpointPath = "/api/v3/chat/completions";

    @Autowired
    public DoubaoProvider(LlmProperties props, RestTemplateBuilder builder) {
        this.props = props;
        this.objectMapper = new ObjectMapper();

        this.restTemplate = builder
            .requestFactory(() -> {
                var factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
                factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
                return factory;
            })
            .build();
    }

    /**
     * messages: List<Map<String,Object>> where each map contains keys "role" and "content" (String)
     */
    @Override
    public String chat(List<Map<String, Object>> messages) throws Exception {
        if (props.getBaseUrl() == null || props.getApiKey() == null || props.getModel() == null) {
            throw new IllegalStateException("LLM configuration (baseUrl/apiKey/model) is not set for DoubaoProvider");
        }

        String url = props.getBaseUrl().replaceAll("/+$", "") + endpointPath;
        log.debug("DoubaoProvider calling URL: {}", url);

        // Build request body following Doubao sample format
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", props.getModel());
        requestBody.put("max_completion_tokens", 1024);
        requestBody.put("reasoning_effort", "medium");

        List<Map<String, Object>> doubaoMessages = new ArrayList<>(messages.size());
        for (Map<String, Object> m : messages) {
            String role = Objects.toString(m.get("role"), "user");
            Object contentObj = m.get("content");
            String text = contentObj == null ? "" : contentObj.toString();

            // Doubao expects content as an array of parts; here we provide a single text part
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", text);

            List<Map<String, Object>> contentArray = Collections.singletonList(textPart);

            Map<String, Object> msg = new HashMap<>();
            msg.put("role", role);
            msg.put("content", contentArray);
            doubaoMessages.add(msg);
        }
        requestBody.put("messages", doubaoMessages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> respEntity;
        try {
            respEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
        } catch (RestClientException rex) {
            log.error("Failed to call Doubao endpoint", rex);
            throw new RuntimeException("调用 Doubao 接口失败: " + rex.getMessage(), rex);
        }

        if (respEntity.getStatusCode() != HttpStatus.OK && respEntity.getStatusCode() != HttpStatus.CREATED) {
            String statusMsg = "Doubao returned non-OK status: " + respEntity.getStatusCode();
            log.error(statusMsg);
            throw new RuntimeException(statusMsg);
        }

        Map<String, Object> body = respEntity.getBody();
        if (body == null) {
            throw new RuntimeException("Doubao 返回空 body");
        }

        // Attempt robust parsing using ObjectMapper to navigate possible shapes
        try {
            JsonNode root = objectMapper.convertValue(body, JsonNode.class);

            // Common shape: { choices: [ { content: [ { type: 'text', text: '...' }, ... ] , ... } ] }
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode c0 = choices.get(0);

                // try c0.content[] -> find first text part
                JsonNode contentNode = c0.path("content");
                if (contentNode.isArray()) {
                    for (JsonNode part : contentNode) {
                        if (part.path("type").asText("").equals("text") && part.has("text")) {
                            return part.get("text").asText();
                        }
                        // some providers use 'content' items with nested 'text' field
                        if (part.has("text")) {
                            return part.get("text").asText();
                        }
                    }
                }

                // try c0.message.content[] (another possible nesting)
                JsonNode messageNode = c0.path("message");
                if (messageNode.isObject()) {
                    JsonNode mContent = messageNode.path("content");
                    if (mContent.isArray()) {
                        for (JsonNode part : mContent) {
                            if (part.path("type").asText("").equals("text") && part.has("text")) {
                                return part.get("text").asText();
                            }
                            if (part.has("text")) {
                                return part.get("text").asText();
                            }
                        }
                    }
                    // or message.content might be a simple string
                    if (mContent.isTextual()) {
                        return mContent.asText();
                    }
                }

                // fallback: choices[0].text (older style)
                if (c0.has("text") && c0.get("text").isTextual()) {
                    return c0.get("text").asText();
                }
            }

            // Alternative: top-level 'result' or 'reply' fields (try to be flexible)
            if (root.has("result") && root.get("result").isTextual()) {
                return root.get("result").asText();
            }
            if (root.has("reply") && root.get("reply").isTextual()) {
                return root.get("reply").asText();
            }

            // Try to find any nested text field anywhere (best-effort)
            Optional<String> anyText = findFirstText(root);
            if (anyText.isPresent()) return anyText.get();

        } catch (IllegalArgumentException ex) {
            log.warn("解析 Doubao 返回时遇到异常，尝试回退解析: {}", ex.getMessage(), ex);
        }

        // Last resort: attempt to stringify body
        try {
            String asString = objectMapper.writeValueAsString(body);
            log.warn("无法解析 Doubao 返回结构，返回原始 JSON 字符串（长度 {}）", asString.length());
            return asString;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("无法解析 Doubao 返回并且无法序列化 body", ex);
        }
    }

    // Walk JSON tree to find first text value (depth-first)
    private Optional<String> findFirstText(JsonNode node) {
        if (node == null || node.isMissingNode()) return Optional.empty();
        if (node.isTextual()) return Optional.of(node.asText());
        if (node.isContainerNode()) {
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                JsonNode child = it.next();
                Optional<String> found = findFirstText(child);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }
}
