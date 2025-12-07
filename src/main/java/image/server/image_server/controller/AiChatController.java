package image.server.image_server.controller;

import image.server.image_server.security.JwtUtil;
import image.server.image_server.service.LlmService;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    @Autowired
    private LlmService llmService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(Authentication authentication,
            @RequestBody Map<String, Object> body,
            @RequestParam(value = "message", required = false) String messageParam,
            @RequestParam(value = "jwt", required = false) String jwtParam,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        UUID userUuid = null;
        try {
            if (authentication != null) {
                userUuid = UUID.fromString(authentication.getName());
            } else if (jwtParam != null && !jwtParam.isBlank()) {
                userUuid = UUID.fromString(jwtUtil.validateAndGetSubject(jwtParam));
            } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                userUuid = UUID.fromString(jwtUtil.validateAndGetSubject(token));
            }
        } catch (Exception ignored) {}

        String userMessage = null;
        if (messageParam != null && !messageParam.isBlank()) {
            userMessage = messageParam;
        } else if (body != null) {
            String[] keys = new String[]{"message","msg","text","content","prompt","q"};
            for (String k : keys) {
                Object v = body.get(k);
                if (v != null) {
                    String s = v.toString().trim();
                    if (!s.isEmpty()) { userMessage = s; break; }
                }
            }
            if (userMessage == null) {
                for (Object v : body.values()) {
                    if (v != null) {
                        String s = v.toString().trim();
                        if (!s.isEmpty()) { userMessage = s; break; }
                    }
                }
            }
        }
        if (userMessage == null || userMessage.trim().isEmpty()) {
            userMessage = " ";
        }

        try {
            String reply = llmService.chat(userUuid, userMessage);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}
