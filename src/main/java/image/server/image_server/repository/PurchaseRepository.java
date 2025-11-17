package image.server.image_server.repository;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import image.server.image_server.model.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByUserUuidAndWallpaperUuid(UUID userUuid, UUID wallpaperUuid);
    boolean existsByUserUuidAndWallpaperUuid(UUID userUuid, UUID wallpaperUuid);

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    @Query("select coalesce(sum(p.priceCents),0) from Purchase p where p.createdAt between :start and :end")
    Long sumPriceCentsBetween(OffsetDateTime start, OffsetDateTime end);

    @Query("select coalesce(sum(p.priceCents),0) from Purchase p")
    Long totalRevenueCents();
}
