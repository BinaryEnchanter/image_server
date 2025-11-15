// java d:\study\空间壁纸\image_server\src\main\java\image\server\image_server\model\UserActionLog.java
package image.server.image_server.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_action_logs")
public class UserActionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private UUID userUuid;

    @Column(nullable=false)
    private String action;

    @Column
    private UUID targetUuid;

    @Column(columnDefinition = "jsonb")
    private String meta;

    @Column(nullable=false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() { createdAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public UUID getUserUuid() { return userUuid; }
    public void setUserUuid(UUID userUuid) { this.userUuid = userUuid; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public UUID getTargetUuid() { return targetUuid; }
    public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}