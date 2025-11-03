package image.server.image_server.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 购买记录（金币购买）
 */
@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private java.util.UUID userUuid;

    @Column
    private java.util.UUID wallpaperUuid;

    @Column
    private Integer priceCents;

    @Column
    private String currency = "coins";

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
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
