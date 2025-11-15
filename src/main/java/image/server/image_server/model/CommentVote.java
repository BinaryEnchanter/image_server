package image.server.image_server.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comment_votes")
public class CommentVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long commentId;

    @Column(nullable=false)
    private UUID userUuid;

    @Column(nullable=false)
    private Short vote;

    @Column
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() { createdAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public UUID getUserUuid() { return userUuid; }
    public void setUserUuid(UUID userUuid) { this.userUuid = userUuid; }
    public Short getVote() { return vote; }
    public void setVote(Short vote) { this.vote = vote; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}