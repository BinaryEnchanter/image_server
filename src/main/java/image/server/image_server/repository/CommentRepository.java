package image.server.image_server.repository;

import image.server.image_server.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByWallpaperUuidOrderByCreatedAtDesc(UUID wallpaperUuid, Pageable pageable);
    List<Comment> findByParentId(Long parentId);
}