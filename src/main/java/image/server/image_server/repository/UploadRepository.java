package image.server.image_server.repository;

import image.server.image_server.model.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;

public interface UploadRepository extends JpaRepository<Upload, Long> {
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
    long countByStatusAndCreatedAtBetween(String status, OffsetDateTime start, OffsetDateTime end);
    long countByStatus(String status);
}
