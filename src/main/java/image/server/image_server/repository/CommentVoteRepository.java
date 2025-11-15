package image.server.image_server.repository;

import image.server.image_server.model.CommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {
    Optional<CommentVote> findByCommentIdAndUserUuid(Long commentId, UUID userUuid);
}