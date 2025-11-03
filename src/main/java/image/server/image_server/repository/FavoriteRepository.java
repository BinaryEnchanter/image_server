package image.server.image_server.repository;

import image.server.image_server.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserUuidAndWallpaperUuid(UUID userUuid, UUID wallpaperUuid);
    long countByWallpaperUuid(UUID wallpaperUuid);
}
