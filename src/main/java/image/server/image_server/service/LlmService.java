package image.server.image_server.service;

import image.server.image_server.config.LlmProperties;
import image.server.image_server.model.ChatMessage;
import image.server.image_server.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LlmService {

    @Autowired
    private LlmProperties props;

    @Autowired
    private ChatMessageRepository chatRepo;

    // 注入 DoubaoProvider by name
    @Autowired
    private LlmProvider doubaoProvider;

    // per-user simple rate limiter (in-memory, not for distributed)
    private final Map<String, Integer> counter = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Long> resetAt = Collections.synchronizedMap(new HashMap<>());

    public String chat(UUID userUuid, String userMessage) throws Exception {
        String key = userUuid == null ? "anon" : userUuid.toString();
        if (!allowRequest(key)) throw new RuntimeException("请求过多，请稍后重试");

        List<Map<String,Object>> messages = new ArrayList<>();
        // system
        if (props.getSystemPrompt() != null && !props.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", props.getSystemPrompt()));
        }

        // history
        if (props.isUseHistory() && userUuid != null) {
            List<ChatMessage> history = chatRepo.findByUserUuidOrderByCreatedAtDesc(userUuid, PageRequest.of(0, props.getHistoryWindow()));
            Collections.reverse(history);
            for (ChatMessage m : history) {
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }

        // current user
        messages.add(Map.of("role", "user", "content", userMessage));

        // call provider
        String reply = doubaoProvider.chat(messages);

        // save history
        if (props.isUseHistory() && userUuid != null) {
            ChatMessage um = new ChatMessage();
            um.setUserUuid(userUuid); um.setRole("user"); um.setContent(userMessage);
            chatRepo.save(um);
            ChatMessage am = new ChatMessage();
            am.setUserUuid(userUuid); am.setRole("assistant"); am.setContent(reply);
            chatRepo.save(am);
        }

        return reply;
    }

    private boolean allowRequest(String key) {
        long now = System.currentTimeMillis();
        long reset = resetAt.getOrDefault(key, 0L);
        if (now > reset) {
            counter.put(key, 0);
            resetAt.put(key, now + 60_000L);
        }
        int c = counter.getOrDefault(key, 0) + 1;
        counter.put(key, c);
        return c <= props.getRateLimitPerMinute();
    }
}
