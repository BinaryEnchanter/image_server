package image.server.image_server.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import image.server.image_server.controller.dto.PagedResponse;
import image.server.image_server.controller.dto.WallpaperDto;
import image.server.image_server.model.User;
import image.server.image_server.model.Wallpaper;
import image.server.image_server.security.JwtUtil;
import image.server.image_server.service.UserService;
import image.server.image_server.service.WallpaperService;
import image.server.image_server.service.ActionLogService;
import io.jsonwebtoken.JwtException;

/**
 * 壁纸相关 API：列表、详情、上传、下载、收藏、购买、搜索
 */
@RestController
@RequestMapping("/api/v1/wallpapers")
public class WallpaperController {

    @Autowired
    private WallpaperService wallpaperService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ActionLogService actionLogService;

    @Value("${app.server.base-url:http://47.109.41.86:8080}")
    private String serverBaseUrl;
    // 列表（分页，每页9条）
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "1") int page) {
        int perPage = 9;
        Page<Wallpaper> p = wallpaperService.listPublic(Math.max(0, page - 1), perPage);
        List<WallpaperDto> items = p.getContent().stream().map(w -> {
            String thumbPath = w.getThumbPath() == null ? "" : w.getThumbPath();
            // 如果 thumbPath 是相对 path（如 "wallpapers/uuid/thumb.jpg"），我们要确保它以 / 开头
            if (!thumbPath.startsWith("/")) thumbPath = "/" + thumbPath;
            // 去掉 serverBaseUrl 尾部的斜杠（若有），再拼接
            String base = serverBaseUrl.replaceAll("/+$", "");
            String thumbUrl = base + "/files" + thumbPath; // 如果你希望用 /files/ + w.getThumbPath()
            // 注意：如果你之前把 thumbPath 存为 "wallpapers/uuid/thumb.jpg", result => http://host:port/files/wallpapers/uuid/thumb.jpg
            return new WallpaperDto(w.getUuid(), w.getName(), thumbUrl, w.getPaid(), w.getPriceCents());
        }).collect(Collectors.toList());

        return ResponseEntity.ok(new PagedResponse<>(page, perPage, p.getTotalElements(), items));
    }

    // 示例：详情接口也返回绝对 URL
    @GetMapping("/{uuid}")
    public ResponseEntity<?> detail(@PathVariable UUID uuid, Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body("unauthenticated");
        UUID userUuid = UUID.fromString(authentication.getName());
        Optional<Wallpaper> o = wallpaperService.findByUuid(uuid);
        if (o.isEmpty())
            return ResponseEntity.status(404).body("not found");
        Wallpaper w = o.get();

        String thumbPath = w.getThumbPath() == null ? "" : w.getThumbPath();
        if (!thumbPath.startsWith("/"))
            thumbPath = "/" + thumbPath;
        String base = serverBaseUrl.replaceAll("/+$", "");
        String thumbUrl = base + "/files" + thumbPath;

        boolean isFavoriteByCurUser=wallpaperService.checkfavorite(userUuid, uuid);
        Map<String, Object> resp = new HashMap<>();
        resp.put("uuid", w.getUuid());
        resp.put("owner_uuid", w.getOwnerUuid());
        resp.put("name", w.getName());
        resp.put("description", w.getDescription());
        resp.put("tags", w.getTags());
        resp.put("thumb_url", thumbUrl);
        resp.put("original_path", base + "/files/"
                + (w.getStoragePath().startsWith("/") ? w.getStoragePath().substring(1) : w.getStoragePath()));
        resp.put("paid", w.getPaid());
        resp.put("price_cents", w.getPriceCents());
        resp.put("size", w.getSizeBytes());
        resp.put("download_count", w.getDownloadCount());
        resp.put("favorite_count", w.getFavoriteCount());
        resp.put("favorite", isFavoriteByCurUser);
        return ResponseEntity.ok(resp);
    }
    
    
    @DeleteMapping("/{uuid}")
public ResponseEntity<?> deleteWallpaper(@PathVariable UUID uuid,
        @RequestHeader("Authorization") String authHeader) {
    try {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }
        String token = authHeader.substring(7);
        String subj = jwtUtil.validateAndGetSubject(token);
        UUID userUuid = UUID.fromString(subj);

        Optional<User> uOpt = userService.findByUuid(userUuid);
        boolean isAdmin = uOpt.map(u -> "admin".equalsIgnoreCase(u.getRole())).orElse(false);

        Wallpaper wp = wallpaperService.findByUuid(uuid).orElseThrow(() -> new RuntimeException("not found"));

        if (!userUuid.equals(wp.getOwnerUuid()) && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }

        // 执行删除（删除文件 + DB 记录）
        wallpaperService.deleteWallpaper(wp);
        actionLogService.log(userUuid, "cancel_upload", uuid, null);

        return ResponseEntity.ok(Map.of("success", true));
    } catch (JwtException jex) {
        return ResponseEntity.status(401).body(Map.of("error", "invalid token"));
    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }
}

    // 上传（需登录）
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("image") MultipartFile file,
                                    @RequestParam(value = "name", required = false) String name,
                                    @RequestParam(value = "tags", required = false) String tags,
                                     @RequestParam(value = "price", required = false, defaultValue = "0") int price,
            @RequestParam("jwt") String jwtToken) {
        System.out.println("JWT: " + jwtToken);                
        if (jwtToken == null || jwtToken.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("api", "ans_upload", "yes_or_no", false, "error", "unauthenticated"));
        }
        System.out.println("JWT: " + jwtToken);
        UUID userUuid = UUID.fromString(jwtUtil.validateAndGetSubject(jwtToken));
        if (userService.isBlacklisted(userUuid)) {
            return ResponseEntity.status(403)
                    .body(Map.of("api", "ans_upload", "yes_or_no", false, "error", "黑名单用户禁止上传"));
        }
        try {
            Wallpaper wp = wallpaperService.upload(userUuid, file, name, tags,price);
            Map<String, Object> r = new HashMap<>();
            r.put("api", "ans_upload");
            r.put("yes_or_no", true);
            r.put("wallpaper_uuid", wp.getUuid());
            actionLogService.log(userUuid, "upload", wp.getUuid(), null);
            return ResponseEntity.ok(r);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("api", "ans_upload", "yes_or_no", false, "error", ex.getMessage()));
        }
    }

    // 下载（需要登录，若付费需购买）
    @GetMapping("/{uuid}/download")
    public ResponseEntity<?> download(@PathVariable UUID uuid, Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body("unauthenticated");
        UUID userUuid = UUID.fromString(authentication.getName());
        if (userService.isBlacklisted(userUuid)) {
            return ResponseEntity.status(403).body("黑名单用户禁止下载");
        }
        Wallpaper wp = wallpaperService.findByUuid(uuid).orElseThrow(() -> new RuntimeException("not found"));

        boolean isowner=wp.getOwnerUuid().equals(userUuid);
        if (!isowner) {
            boolean purchased = wallpaperService.hasPurchased(userUuid, uuid);
            if (!purchased) {
                return ResponseEntity.status(403).body("请先购买此壁纸才能下载");
            }
        }

        try {
            wallpaperService.handleDownload(userUuid, wp,isowner);

            String base = serverBaseUrl.replaceAll("/+$", "");
            String downloadUrl = base + "/files/"
                    + (wp.getStoragePath().startsWith("/") ? wp.getStoragePath().substring(1) : wp.getStoragePath());
            actionLogService.log(userUuid, "download", uuid, null);
            return ResponseEntity.ok(Map.of("download_url", downloadUrl));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ex.getMessage());
        }
    }


    // 收藏（需要登录）
    @PostMapping("/{uuid}/favorite")
    public ResponseEntity<?> favorite(@PathVariable UUID uuid, @RequestParam("jwt") String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("api", "ans_upload", "yes_or_no", false, "error", "unauthenticated"));
        }
        System.out.println("JWT: " + jwtToken);
        UUID userUuid = UUID.fromString(jwtUtil.validateAndGetSubject(jwtToken));
        wallpaperService.favorite(userUuid, uuid);
        actionLogService.log(userUuid, "favorite", uuid, null);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{uuid}/favorite")
    public ResponseEntity<?> unfavorite(@PathVariable UUID uuid, @RequestParam("jwt") String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }
        try {
            UUID userUuid = UUID.fromString(jwtUtil.validateAndGetSubject(jwtToken));
            wallpaperService.unfavorite(userUuid, uuid);
            actionLogService.log(userUuid, "unfavorite", uuid, null);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (JwtException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 是否已经购买
    @GetMapping("/{uuid}/check-purchase")
    public ResponseEntity<?> checkPurchase(@PathVariable UUID uuid, Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body("unauthenticated");
        UUID userUuid = UUID.fromString(authentication.getName());
        boolean purchased = wallpaperService.hasPurchased(userUuid, uuid);
        return ResponseEntity.ok(Map.of("purchased", purchased));
    }

    // 购买（金币）简单接口
    @PostMapping("/{uuid}/purchase")
    public ResponseEntity<?> purchase(@PathVariable UUID uuid, Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body("unauthenticated");
        UUID userUuid = UUID.fromString(authentication.getName());
        try {
            wallpaperService.purchase(userUuid, uuid);
            actionLogService.log(userUuid, "purchase", uuid, null);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(ex.getMessage());
        }
    }

    // Buy (purchase image)
    @PostMapping("/{uuid}/buy")
    public ResponseEntity<?> buyWallpaper(@PathVariable UUID uuid, Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        UUID userUuid = UUID.fromString(authentication.getName());
        try {
            wallpaperService.purchaseImage(userUuid, uuid);
            actionLogService.log(userUuid, "purchase", uuid, null);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "internal error"));
        }
    }

    // 搜索
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam(defaultValue = "1") int page) {
        int perPage = 9;
        Page<Wallpaper> p = wallpaperService.search(q, Math.max(0, page - 1), perPage);
        List<WallpaperDto> items = p.getContent().stream().map(w ->
                new WallpaperDto(w.getUuid(), w.getName(), "http://47.109.41.86:8080/files/" + w.getThumbPath(), w.getPaid(), w.getPriceCents())
        ).collect(Collectors.toList());
        return ResponseEntity.ok(new PagedResponse<>(page, perPage, p.getTotalElements(), items));
    }
}
