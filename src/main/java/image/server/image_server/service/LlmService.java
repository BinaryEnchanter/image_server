package image.server.image_server.service;

import image.server.image_server.config.LlmProperties;
import image.server.image_server.model.ChatMessage;
import image.server.image_server.repository.ChatMessageRepository;
import image.server.image_server.service.UserService;
import image.server.image_server.model.User;
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

    @Autowired
    private UserService userService;

    // 注入 DoubaoProvider by name
    @Autowired
    private LlmProvider doubaoProvider;

    // per-user simple rate limiter (in-memory, not for distributed)
    private final Map<String, Integer> counter = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Long> resetAt = Collections.synchronizedMap(new HashMap<>());

    public String chat(UUID userUuid, String userMessage) throws Exception {
        String key = userUuid == null ? "anon" : userUuid.toString();
        if (!allowRequest(key)) {}

        List<Map<String,Object>> messages = new ArrayList<>();
        // system
        if (props.getSystemPrompt() != null && !props.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", props.getSystemPrompt()));
        }
        if (userUuid != null) {
            Optional<User> uOpt = userService.findByUuid(userUuid);
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                String profile = "用户信息：uuid=" + u.getUuid()
                        + "；用户名=" + u.getUsername()
                        + "；邮箱=" + (u.getEmail() == null ? "" : u.getEmail())
                        + "；角色=" + u.getRole()
                        + "；金币=" + (u.getCoins() == null ? 0L : u.getCoins());
                messages.add(Map.of("role", "system", "content", profile));
            }
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

        String reply = null;
        Exception lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            List<Map<String,Object>> msgs;
            if (attempt == 1) {
                msgs = messages;
            } else if (attempt == 2) {
                msgs = new ArrayList<>();
                if (props.getSystemPrompt() != null && !props.getSystemPrompt().isBlank()) {
                    msgs.add(Map.of("role", "system", "content", props.getSystemPrompt()));
                }
                msgs.add(Map.of("role", "user", "content", userMessage));
            } else {
                msgs = List.of(Map.of("role", "user", "content", userMessage));
            }
            try {
                reply = doubaoProvider.chat(msgs);
                break;
            } catch (Exception ex) {
                lastEx = ex;
                try { Thread.sleep(300L * attempt); } catch (InterruptedException ignored) {}
            }
        }
        if (reply == null) {
            throw (lastEx instanceof RuntimeException ? (RuntimeException) lastEx : new RuntimeException(lastEx));
        }

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
