package image.server.image_server.service;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import image.server.image_server.model.Favorite;
import image.server.image_server.model.Purchase;
import image.server.image_server.model.Upload;
import image.server.image_server.model.User;
import image.server.image_server.model.Wallpaper;
import image.server.image_server.repository.FavoriteRepository;
import image.server.image_server.repository.PurchaseRepository;
import image.server.image_server.repository.UploadRepository;
import image.server.image_server.repository.UserRepository;
import image.server.image_server.repository.WallpaperRepository;

/**
 * WallpaperService：处理上传、下载计数、购买、收藏等逻辑
 */
@Service
public class WallpaperService {

    @Autowired
    private WallpaperRepository wallpaperRepository;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Value("${app.moderation.enabled:false}")
    private boolean moderationEnabled;

    @Value("${app.moderation.check-url:}")
    private String moderationUrl;

    @Value("${app.moderation.api-key:}")
    private String moderationApiKey;

    /**
     * 分页查询墙纸（公共可见）
     */
    public Page<Wallpaper> listPublic(int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return wallpaperRepository.findByVisibility("public", p);
    }

    /**
     * 根据owner查壁纸
     */
    public Page<Wallpaper> listByOwner(UUID ownerUuid, int page, int perPage) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.by("createdAt").descending());
        return wallpaperRepository.findAllByOwnerUuid(ownerUuid, pageable);
    }

    /**
     * 搜索（name 或 tags）
     */
    public Page<Wallpaper> search(String keyword, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return wallpaperRepository.findByNameContainingIgnoreCaseOrTagsContainingIgnoreCaseAndVisibility(keyword, keyword, "public", p);
    }

    /**
     * 上传并存储：返回 Wallpaper （metadata 存入 DB），同步生成缩略图（简化）
     */
    @Transactional
    public Wallpaper upload(UUID userUuid, MultipartFile file, String name, String tags, int price) throws IOException {
        Upload up = new Upload();
        up.setUserUuid(userUuid);
        up.setOriginalFilename(file.getOriginalFilename());
        up.setStatus("processing");
        uploadRepository.save(up);

        if (!reviewImage(file)) {
            up.setStatus("failed");
            up.setErrorMsg("rejected by moderation");
            uploadRepository.save(up);
            throw new IOException("图片未通过内容安全审核");
        }

        Wallpaper wp = new Wallpaper();
        wp.setOwnerUuid(userUuid);
        wp.setName(name == null ? file.getOriginalFilename() : name);
        wp.setDescription("");
        wp.setTags(tags == null ? "" : tags);
        UUID id = UUID.randomUUID();
        wp.setUuid(id);
        String ext = org.apache.commons.io.FilenameUtils.getExtension(file.getOriginalFilename());
        if (ext == null || ext.isBlank()) ext = "jpg";
        String originalPath = String.format("wallpapers/%s/original.%s", id, ext);
        String thumbPath = String.format("wallpapers/%s/thumb.jpg", id);
        wp.setStoragePath(originalPath);
        wp.setThumbPath(thumbPath);
        wp.setSizeBytes(file.getSize());
        wp.setPaid(price > 0);
        wp.setPriceCents(price);
        wp.setVisibility("public");
        wallpaperRepository.save(wp);

        // 3. store file and create thumbnail (synchronous here)
        storageService.store(file, originalPath);
        storageService.generateThumbnail(originalPath, thumbPath, 600, 600);

        // 4. update upload-> wallpaperUuid and status
        up.setWallpaperUuid(wp.getUuid());
        up.setStatus("done");
        uploadRepository.save(up);

        return wp;
    }

    public Optional<Wallpaper> findByUuid(UUID uuid) {
        return wallpaperRepository.findById(uuid);
    }

    /**
     * 下载前的权限检查：若 paid 且未购买且不是上传者则抛异常；成功则增加 download_count（事务）
     */
    @Transactional
    public void handleDownload(UUID userUuid, Wallpaper wallpaper,boolean isowner) {
        if (wallpaper.getPaid()&&!isowner) {
            Optional<Purchase> p = purchaseRepository.findByUserUuidAndWallpaperUuid(userUuid, wallpaper.getUuid());
            if (p.isEmpty()) {
                throw new RuntimeException("wallpaper is paid; purchase required");
            }
        }
        // 增加下载计数（简单实现）
        wallpaper.setDownloadCount(wallpaper.getDownloadCount() + 1);
        wallpaperRepository.save(wallpaper);
    }

    public Resource loadAsResource(String logicalPath) throws IOException {
        return storageService.loadAsResource(logicalPath);
    }


    /**
     * 检查是否收藏
     */
    @Transactional
    public boolean checkfavorite(UUID userUuid, UUID wallpaperUuid) {
        return favoriteRepository.findByUserUuidAndWallpaperUuid(userUuid, wallpaperUuid).isPresent();

        
    }

    /**
     * 收藏（如果已收藏则无操作）
     */
    @Transactional
    public void favorite(UUID userUuid, UUID wallpaperUuid) {
        if (favoriteRepository.findByUserUuidAndWallpaperUuid(userUuid, wallpaperUuid).isPresent())
            return;
        Favorite f = new Favorite();
        f.setUserUuid(userUuid);
        f.setWallpaperUuid(wallpaperUuid);
        favoriteRepository.save(f);
        // update count
        long cnt = favoriteRepository.countByWallpaperUuid(wallpaperUuid);
        wallpaperRepository.findById(wallpaperUuid).ifPresent(w -> {
            w.setFavoriteCount(cnt);
            wallpaperRepository.save(w);
        });
    }

    /**
     * 取消收藏
     */
    @Transactional
    public void unfavorite(UUID userUuid, UUID wallpaperUuid) {
        // 查找是否存在收藏记录
        Optional<Favorite> existing = favoriteRepository.findByUserUuidAndWallpaperUuid(userUuid, wallpaperUuid);
        if (existing.isEmpty()) return; // 未收藏则忽略

        // 删除收藏记录
        favoriteRepository.delete(existing.get());

        // 更新收藏次数
        long cnt = favoriteRepository.countByWallpaperUuid(wallpaperUuid);
        wallpaperRepository.findById(wallpaperUuid).ifPresent(w -> {
            w.setFavoriteCount(cnt);
            wallpaperRepository.save(w);
        });
    }
    /**
     * 购买（金币支付）：在事务内完成检查->扣款->写 purchases
     */
    @Transactional
    public void purchase(UUID userUuid, UUID wallpaperUuid) {
        Wallpaper wp = wallpaperRepository.findById(wallpaperUuid)
                .orElseThrow(() -> new RuntimeException("wallpaper not found"));
        if (!wp.getPaid())
            throw new RuntimeException("not a paid wallpaper");
        User u = userRepository.findById(userUuid).orElseThrow(() -> new RuntimeException("user not found"));
        long priceCoins = wp.getPriceCents(); // priceCents 当作 coins（简化）
        if (u.getCoins() < priceCoins)
            throw new RuntimeException("not enough coins");
        u.setCoins(u.getCoins() - priceCoins);
        userRepository.save(u);

        Purchase purchase = new Purchase();
        purchase.setUserUuid(userUuid);
        purchase.setWallpaperUuid(wallpaperUuid);
        purchase.setPriceCents((int) priceCoins);
        purchaseRepository.save(purchase);
    }
    
    /**
     * 购买（金币支付）：在事务内完成检查->扣款->写 purchases
     */
     @Transactional
     public void purchaseImage(UUID userUuid, UUID wallpaperUuid) {
         // 1. check wallpaper exists
         Wallpaper w = wallpaperRepository.findById(wallpaperUuid)
                 .orElseThrow(() -> new RuntimeException("wallpaper not found"));

         // 2. if purchase exists, return (idempotent)
         if (purchaseRepository.existsByUserUuidAndWallpaperUuid(userUuid, wallpaperUuid)) {
             return; // already purchased — do nothing
         }

         // 3. get user with row lock to avoid concurrent deductions
         User user = userRepository.findByUuidForUpdate(userUuid)
                 .orElseThrow(() -> new RuntimeException("user not found"));

         int price = w.getPriceCents(); // price in coins (as cents) — follow your convention

         // 4. check balance (treat price 0 as always ok)
         long currentCoins = user.getCoins() == null ? 0L : user.getCoins();
         if (currentCoins < price) {
             throw new RuntimeException("insufficient coins");
         }

         // 5. deduct coins and save user
         user.setCoins(currentCoins - price);
         userRepository.save(user);

         // 6. write purchase record
         Purchase p = new Purchase();
         p.setUserUuid(userUuid);
         p.setWallpaperUuid(wallpaperUuid);
         p.setPriceCents(price);
         p.setCurrency("coins");
         purchaseRepository.save(p);

         // (optional) increment purchase_count on wallpaper / audit etc.
     }
    
    /**
     * 撤回上传
     */
    @Transactional
    public void deleteWallpaper(Wallpaper wp) {
        try {
            if (wp.getStoragePath() != null)
                storageService.delete(wp.getStoragePath());
            if (wp.getThumbPath() != null)
                storageService.delete(wp.getThumbPath());
        } catch (Exception e) {
            // log but continue to delete DB record (或根据策略回退)
            System.err.println("failed to delete files");
        }
        wallpaperRepository.delete(wp);
    }

    public boolean hasPurchased(UUID userUuid, UUID wallpaperUuid) {
        return purchaseRepository.findByUserUuidAndWallpaperUuid(userUuid, wallpaperUuid).isPresent();
    }

    private boolean reviewImage(MultipartFile file) {
        if (!moderationEnabled || moderationUrl == null || moderationUrl.isBlank()) return true;
        long size = file.getSize();
        if (size < 5 * 1024 || size > 4 * 1024 * 1024) return false;
        try {
            RestTemplate rt = restTemplateBuilder.build();
            String url = moderationUrl;
            if (!url.contains("access_token")) {
                if (moderationApiKey == null || moderationApiKey.isBlank()) return false;
                url = url + (url.contains("?") ? "&" : "?") + "access_token=" + moderationApiKey;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            String b64 = Base64.getEncoder().encodeToString(file.getBytes());
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("image", b64);
            form.add("strategyId", "1");
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
            ResponseEntity<String> resp = rt.postForEntity(url, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                ObjectMapper om = new ObjectMapper();
                JsonNode root = om.readTree(resp.getBody());
                JsonNode t = root.path("conclusionType");
                return t.isInt() && t.asInt() == 1;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
