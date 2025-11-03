package image.server.image_server.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import image.server.image_server.controller.dto.PagedResponse;
import image.server.image_server.controller.dto.WallpaperDto;
import image.server.image_server.model.User;
import image.server.image_server.model.Wallpaper;
import image.server.image_server.repository.WallpaperRepository;
import image.server.image_server.security.JwtUtil;
import image.server.image_server.service.UserService;
import image.server.image_server.service.WallpaperService;

/**
 * 用户相关接口：获取当前用户信息，显示拥有资产缩略图（简化）
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private WallpaperRepository wallpaperRepository;

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body("unauthenticated");
        try {
            UUID uuid = UUID.fromString(authentication.getName());
            User u = userService.findByUuid(uuid).orElseThrow();
            // 简单返回用户基本信息与金币
            return ResponseEntity.ok(u);
        } catch (Exception ex) {
            return ResponseEntity.status(400).body("invalid user");
        }
    }
    
     @Autowired
    private WallpaperService wallpaperService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.server.base-url:http://47.109.41.86:8080}")
    private String serverBaseUrl;

    // 获取当前用户上传的壁纸列表（分页可选）
    @GetMapping("/me/wallpapers")
    public ResponseEntity<?> myWallpapers(@RequestHeader("Authorization") String authHeader,
                                          @RequestParam(defaultValue = "1") int page) {
        try {
            // 从 JWT 获取用户 UUID
            String jwt = authHeader.replace("Bearer ", "");
            UUID userUuid = UUID.fromString(jwtUtil.validateAndGetSubject(jwt));

            int perPage = 9;
            Page<Wallpaper> p = wallpaperService.listByOwner(userUuid, Math.max(0, page - 1), perPage);

            List<WallpaperDto> items = p.getContent().stream().map(w -> {
                String thumbPath = w.getThumbPath() == null ? "" : w.getThumbPath();
                if (!thumbPath.startsWith("/")) thumbPath = "/" + thumbPath;
                String base = serverBaseUrl.replaceAll("/+$", "");
                String thumbUrl = base + "/files" + thumbPath;
                return new WallpaperDto(w.getUuid(), w.getName(), thumbUrl, w.getPaid(), w.getPriceCents());
            }).collect(Collectors.toList());

            return ResponseEntity.ok(new PagedResponse<>(page, perPage, p.getTotalElements(), items));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }
    }
}
