package image.server.image_server.repository;

import image.server.image_server.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserUuidOrderByCreatedAtDesc(UUID userUuid, Pageable pageable);
}
