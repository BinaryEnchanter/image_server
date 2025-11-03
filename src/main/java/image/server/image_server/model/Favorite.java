package image.server.image_server.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 收藏记录
 */
@Entity
@Table(name = "favorites")
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private java.util.UUID userUuid;

    @Column
    private java.util.UUID wallpaperUuid;

    @Column
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public java.util.UUID getUserUuid() { return userUuid; }
    public void setUserUuid(java.util.UUID userUuid) { this.userUuid = userUuid; }
    public java.util.UUID getWallpaperUuid() { return wallpaperUuid; }
    public void setWallpaperUuid(java.util.UUID wallpaperUuid) { this.wallpaperUuid = wallpaperUuid; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
