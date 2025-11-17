package image.server.image_server.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import image.server.image_server.controller.dto.PagedResponse;
import image.server.image_server.model.User;
import image.server.image_server.model.UserActionLog;
import image.server.image_server.repository.FavoriteRepository;
import image.server.image_server.repository.PurchaseRepository;
import image.server.image_server.repository.UploadRepository;
import image.server.image_server.repository.UserRepository;
import image.server.image_server.repository.WallpaperRepository;
import image.server.image_server.security.JwtUtil;
import image.server.image_server.service.ActionLogService;
import image.server.image_server.service.UserService;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private ActionLogService actionLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private WallpaperRepository wallpaperRepository;

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

    @GetMapping("/users/{username}/logs")
    public ResponseEntity<?> userLogs(@PathVariable String username,
                                      @RequestHeader("Authorization") String authHeader,
                                      @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String token = authHeader.substring(7);
            UUID adminUuid = UUID.fromString(jwtUtil.validateAndGetSubject(token));
            if (!userService.isAdmin(adminUuid))
                return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
            Optional<image.server.image_server.model.User> uOpt = userService.findByUsername(username);
            if (uOpt.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "user not found"));
            image.server.image_server.model.User u = uOpt.get();
            Page<UserActionLog> p = actionLogService.listByUser(u.getUuid(), page - 1, size);
            List<Map<String, Object>> items = p.getContent().stream().map(l -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", l.getId());
                m.put("username", l.getUsername());
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
    
    @GetMapping("/stats")
    public ResponseEntity<?> stats(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body(Map.of("error","unauthenticated"));
        UUID userUuid = UUID.fromString(authentication.getName());
        Optional<User> uOpt = userService.findByUuid(userUuid);
        boolean isAdmin = uOpt.map(u -> "admin".equalsIgnoreCase(u.getRole())).orElse(false);
        if (!isAdmin) return ResponseEntity.status(403).body(Map.of("error","forbidden"));

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startDay = OffsetDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, now.getOffset());
        int dow = startDay.getDayOfWeek().getValue();
        OffsetDateTime startWeek = startDay.minusDays(dow - 1);
        OffsetDateTime startMonth = OffsetDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIDNIGHT, now.getOffset());

        Map<String,Object> today = new HashMap<>();
        today.put("new_users", userRepository.countByCreatedAtBetween(startDay, now));
        today.put("uploads_total", uploadRepository.countByCreatedAtBetween(startDay, now));
        today.put("uploads_passed", uploadRepository.countByStatusAndCreatedAtBetween("done", startDay, now));
        today.put("uploads_failed", uploadRepository.countByStatusAndCreatedAtBetween("failed", startDay, now));
        today.put("purchases", purchaseRepository.countByCreatedAtBetween(startDay, now));
        today.put("favorites", favoriteRepository.countByCreatedAtBetween(startDay, now));
        today.put("revenue_cents", purchaseRepository.sumPriceCentsBetween(startDay, now));
        today.put("wallpapers_created", wallpaperRepository.countByCreatedAtBetween(startDay, now));

        Map<String,Object> week = new HashMap<>();
        week.put("new_users", userRepository.countByCreatedAtBetween(startWeek, now));
        week.put("uploads_total", uploadRepository.countByCreatedAtBetween(startWeek, now));
        week.put("uploads_passed", uploadRepository.countByStatusAndCreatedAtBetween("done", startWeek, now));
        week.put("uploads_failed", uploadRepository.countByStatusAndCreatedAtBetween("failed", startWeek, now));
        week.put("purchases", purchaseRepository.countByCreatedAtBetween(startWeek, now));
        week.put("favorites", favoriteRepository.countByCreatedAtBetween(startWeek, now));
        week.put("revenue_cents", purchaseRepository.sumPriceCentsBetween(startWeek, now));
        week.put("wallpapers_created", wallpaperRepository.countByCreatedAtBetween(startWeek, now));

        Map<String,Object> month = new HashMap<>();
        month.put("new_users", userRepository.countByCreatedAtBetween(startMonth, now));
        month.put("uploads_total", uploadRepository.countByCreatedAtBetween(startMonth, now));
        month.put("uploads_passed", uploadRepository.countByStatusAndCreatedAtBetween("done", startMonth, now));
        month.put("uploads_failed", uploadRepository.countByStatusAndCreatedAtBetween("failed", startMonth, now));
        month.put("purchases", purchaseRepository.countByCreatedAtBetween(startMonth, now));
        month.put("favorites", favoriteRepository.countByCreatedAtBetween(startMonth, now));
        month.put("revenue_cents", purchaseRepository.sumPriceCentsBetween(startMonth, now));
        month.put("wallpapers_created", wallpaperRepository.countByCreatedAtBetween(startMonth, now));

        Map<String,Object> totals = new HashMap<>();
        totals.put("users_total", userRepository.count());
        totals.put("wallpapers_total", wallpaperRepository.count());
        totals.put("public_wallpapers", wallpaperRepository.countByVisibility("public"));
        totals.put("private_wallpapers", wallpaperRepository.countByVisibility("private"));
        totals.put("paid_wallpapers", wallpaperRepository.countByPaid(true));
        totals.put("free_wallpapers", wallpaperRepository.countByPaid(false));
        totals.put("uploads_failed_total", uploadRepository.countByStatus("failed"));
        totals.put("uploads_passed_total", uploadRepository.countByStatus("done"));
        totals.put("downloads_total", wallpaperRepository.totalDownloads());
        totals.put("favorites_total", favoriteRepository.count());
        totals.put("revenue_total_cents", purchaseRepository.totalRevenueCents());

        Map<String,Object> resp = new HashMap<>();
        resp.put("today", today);
        resp.put("week", week);
        resp.put("month", month);
        resp.put("totals", totals);

        return ResponseEntity.ok(resp);
    }
}