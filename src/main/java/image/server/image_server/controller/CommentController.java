package image.server.image_server.controller;

import image.server.image_server.controller.dto.PagedResponse;
import image.server.image_server.model.Comment;
import image.server.image_server.model.User;
import image.server.image_server.service.CommentService;
import image.server.image_server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private UserService userService;

    @GetMapping("/wallpapers/{uuid}/comments")
    public ResponseEntity<?> list(@PathVariable java.util.UUID uuid, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body("unauthenticated");
        Page<Comment> p = commentService.list(uuid, page - 1, size);
        List<Map<String, Object>> items = p.getContent().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("content", c.getContent());
            m.put("like_count", c.getLikeCount());
            m.put("dislike_count", c.getDislikeCount());
            m.put("parent_id", c.getParentId());
            m.put("created_at", c.getCreatedAt());
            m.put("username", userService.findByUuid(c.getUserUuid()).map(User::getUsername).orElse(""));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(new PagedResponse<>(page, size, p.getTotalElements(), items));
    }

    @PostMapping("/wallpapers/{uuid}/comments")
    public ResponseEntity<?> add(@PathVariable java.util.UUID uuid, @RequestBody Map<String, Object> body, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body("unauthenticated");
        java.util.UUID userUuid = java.util.UUID.fromString(authentication.getName());
        String content = Objects.toString(body.get("content"), "").trim();
        if (content.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "content required"));
        Long parentId = null;
        Object p = body.get("parent_id");
        if (p != null) parentId = Long.parseLong(p.toString());
        Comment c = commentService.addComment(userUuid, uuid, content, parentId);
        return ResponseEntity.ok(Map.of("id", c.getId()));
    }

    @PostMapping("/comments/{id}/like")
    public ResponseEntity<?> like(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body("unauthenticated");
        java.util.UUID userUuid = java.util.UUID.fromString(authentication.getName());
        commentService.vote(userUuid, id, 1);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/comments/{id}/dislike")
    public ResponseEntity<?> dislike(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body("unauthenticated");
        java.util.UUID userUuid = java.util.UUID.fromString(authentication.getName());
        commentService.vote(userUuid, id, -1);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body("unauthenticated");
        java.util.UUID actorUuid = java.util.UUID.fromString(authentication.getName());
        boolean isAdmin = userService.isAdmin(actorUuid);
        try {
            commentService.deleteComment(actorUuid, id, isAdmin);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }
}