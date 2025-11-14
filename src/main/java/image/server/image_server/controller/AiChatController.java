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
            @RequestBody Map<String, String> body) {
        
         if (authentication == null)
            return ResponseEntity.status(401).body("unauthenticated");
        UUID userUuid = UUID.fromString(authentication.getName());
                
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "ai：不能发送空信息"));
                                    

        try {
            String reply = llmService.chat(userUuid, userMessage);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}
