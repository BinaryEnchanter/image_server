package image.server.image_server.controller;

import image.server.image_server.controller.dto.PagedResponse;
import image.server.image_server.model.UserActionLog;
import image.server.image_server.security.JwtUtil;
import image.server.image_server.service.ActionLogService;
import image.server.image_server.service.UserService;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private ActionLogService actionLogService;

    @PostMapping("/users/{uuid}/role")
    public ResponseEntity<?> setRole(@PathVariable UUID uuid,
                                     @RequestHeader("Authorization") String authHeader,
                                     @RequestBody Map<String, String> body) {
        try {
            String token = authHeader.substring(7);
            UUID adminUuid = UUID.fromString(jwtUtil.validateAndGetSubject(token));
            if (!userService.isAdmin(adminUuid)) return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
            String role = Optional.ofNullable(body.get("role")).orElse("");
            userService.updateRole(uuid, role);
            return ResponseEntity.ok(Map.of("ok", true, "user_uuid", uuid, "role", role.toLowerCase()));
        } catch (JwtException jex) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid token"));
        } catch (RuntimeException rex) {
            return ResponseEntity.badRequest().body(Map.of("error", rex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/users/{uuid}/logs")
    public ResponseEntity<?> userLogs(@PathVariable UUID uuid,
                                      @RequestHeader("Authorization") String authHeader,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        try {
            String token = authHeader.substring(7);
            UUID adminUuid = UUID.fromString(jwtUtil.validateAndGetSubject(token));
            if (!userService.isAdmin(adminUuid)) return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
            Page<UserActionLog> p = actionLogService.listByUser(uuid, page - 1, size);
            List<Map<String, Object>> items = p.getContent().stream().map(l -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", l.getId());
                m.put("action", l.getAction());
                m.put("target_uuid", l.getTargetUuid());
                m.put("meta", l.getMeta());
                m.put("created_at", l.getCreatedAt());
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(new PagedResponse<>(page, size, p.getTotalElements(), items));
        } catch (JwtException jex) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid token"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}