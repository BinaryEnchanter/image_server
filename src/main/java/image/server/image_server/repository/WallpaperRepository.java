package image.server.image_server.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import image.server.image_server.model.Wallpaper;

public interface WallpaperRepository extends JpaRepository<Wallpaper, UUID> {
    // 简单的包含搜索（name 或 tags）
    Page<Wallpaper> findByVisibility(String visibility, Pageable pageable);

    Page<Wallpaper> findByNameContainingIgnoreCaseOrTagsContainingIgnoreCaseAndVisibility(
            String name, String tags, String visibility, Pageable pageable);

    Page<Wallpaper> findAllByOwnerUuid(UUID ownerUuid, Pageable pageable);
}
