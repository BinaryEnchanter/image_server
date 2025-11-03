package image.server.image_server.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import image.server.image_server.model.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByUserUuidAndWallpaperUuid(UUID userUuid, UUID wallpaperUuid);
     boolean existsByUserUuidAndWallpaperUuid(UUID userUuid, UUID wallpaperUuid);
}
