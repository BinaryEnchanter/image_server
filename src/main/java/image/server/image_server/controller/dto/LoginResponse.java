package image.server.image_server.controller.dto;

import java.util.UUID;

public class LoginResponse {
    private String token;
    private UUID uuid;
    private String username;
    private Long coins;

    public LoginResponse(String token, UUID uuid, String username, Long coins) {
        this.token = token;
        this.uuid = uuid;
        this.username = username;
        this.coins = coins;
    }

    // getters
    public String getToken() { return token; }
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public Long getCoins() { return coins; }
}
