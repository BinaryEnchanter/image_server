// java d:\study\空间壁纸\image_server\src\main\java\image\server\image_server\repository\UserActionLogRepository.java
package image.server.image_server.repository;

import image.server.image_server.model.UserActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserActionLogRepository extends JpaRepository<UserActionLog, Long> {
    Page<UserActionLog> findAllByUserUuidOrderByCreatedAtDesc(UUID userUuid, Pageable pageable);
}