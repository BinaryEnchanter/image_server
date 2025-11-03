package image.server.image_server.controller.dto;

import java.util.UUID;

/**
 * 简单的 Wallpaper DTO：用于列表/详情（包含 URL path）
 */
public class WallpaperDto {
    private UUID uuid;
    private String name;
    private String thumbUrl;
    private boolean paid;
    private int priceCents;

    public WallpaperDto(UUID uuid, String name, String thumbUrl, boolean paid, int priceCents) {
        this.uuid = uuid;
        this.name = name;
        this.thumbUrl = thumbUrl;
        this.paid = paid;
        this.priceCents = priceCents;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public String getThumbUrl() { return thumbUrl; }
    public boolean isPaid() { return paid; }
    public int getPriceCents() { return priceCents; }
}
