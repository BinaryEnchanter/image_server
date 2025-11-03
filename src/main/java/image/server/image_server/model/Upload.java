package image.server.image_server.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 上传记录（简要）
 */
@Entity
@Table(name = "uploads")
public class Upload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private java.util.UUID userUuid;

    @Column
    private java.util.UUID wallpaperUuid;

    @Column
    private String originalFilename;

    @Column
    private String status; // processing/done/failed

    @Column
    private String errorMsg;

    @Column
    private String ip;

    @Column
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        status = status == null ? "processing" : status;
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // getters/setters omitted for brevity
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public java.util.UUID getUserUuid() { return userUuid; }
    public void setUserUuid(java.util.UUID userUuid) { this.userUuid = userUuid; }
    public java.util.UUID getWallpaperUuid() { return wallpaperUuid; }
    public void setWallpaperUuid(java.util.UUID wallpaperUuid) { this.wallpaperUuid = wallpaperUuid; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
