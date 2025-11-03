package image.server.image_server.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wallpaper 元数据实体。tags 存为逗号分隔字符串（简化实现）。
 */
@Entity
@Table(name = "wallpapers")
public class Wallpaper {

    @Id
    private UUID uuid;

    @Column
    private UUID ownerUuid;

    @Column(nullable=false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column
    private String tags; // comma separated

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column
    private Long sizeBytes;

    @Column
    private Long downloadCount = 0L;

    @Column
    private Long favoriteCount = 0L;

    @Column
    private Boolean paid = false;

    @Column
    private Integer priceCents = 0;

    @Column(nullable=false)
    private String storagePath; // logical path relative to base-dir

    @Column
    private String thumbPath;

    @Column
    private String phash;

    @Column
    private String visibility = "public";

    @Column
    private Boolean localOnly = false;

    @Column
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // getters & setters (省略注释；请在真实工程中使用 IDE 生成)
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Long downloadCount) { this.downloadCount = downloadCount; }
    public Long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getThumbPath() { return thumbPath; }
    public void setThumbPath(String thumbPath) { this.thumbPath = thumbPath; }
    public String getPhash() { return phash; }
    public void setPhash(String phash) { this.phash = phash; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Boolean getLocalOnly() { return localOnly; }
    public void setLocalOnly(Boolean localOnly) { this.localOnly = localOnly; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
