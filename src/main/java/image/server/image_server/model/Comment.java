package image.server.image_server.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private UUID userUuid;

    @Column(nullable=false)
    private UUID wallpaperUuid;

    @Column
    private Long parentId;

    @Column(columnDefinition = "text", nullable=false)
    private String content;

    @Column
    private Long likeCount = 0L;

    @Column
    private Long dislikeCount = 0L;

    @Column
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    public void preUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public UUID getUserUuid() { return userUuid; }
    public void setUserUuid(UUID userUuid) { this.userUuid = userUuid; }
    public UUID getWallpaperUuid() { return wallpaperUuid; }
    public void setWallpaperUuid(UUID wallpaperUuid) { this.wallpaperUuid = wallpaperUuid; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    public Long getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(Long dislikeCount) { this.dislikeCount = dislikeCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}