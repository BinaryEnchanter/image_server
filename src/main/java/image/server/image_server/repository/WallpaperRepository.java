package image.server.image_server.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import image.server.image_server.model.Wallpaper;

public interface WallpaperRepository extends JpaRepository<Wallpaper, UUID> {
    Page<Wallpaper> findByVisibility(String visibility, Pageable pageable);

    Page<Wallpaper> findByNameContainingIgnoreCaseOrTagsContainingIgnoreCaseAndVisibility(
            String name, String tags, String visibility, Pageable pageable);

    Page<Wallpaper> findAllByOwnerUuid(UUID ownerUuid, Pageable pageable);

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    long countByVisibility(String visibility);

    long countByPaid(Boolean paid);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(w.downloadCount),0) from Wallpaper w")
    Long totalDownloads();

    Page<Wallpaper> findByVisibilityAndTagsContainingIgnoreCaseAndUuidNot(String visibility, String tags, UUID uuid, Pageable pageable);

    Page<Wallpaper> findByVisibilityAndTagsContainingIgnoreCase(String visibility, String tags, Pageable pageable);
}