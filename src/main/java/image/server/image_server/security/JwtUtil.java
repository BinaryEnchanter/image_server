package image.server.image_server.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * 简单的 JWT 工具类（HS256）
 */
@Component
public class JwtUtil {

    private final Key hmacKey;
    private final long expireSeconds;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-seconds}") long expireSeconds) {
        this.hmacKey = Keys.hmacShaKeyFor(secret.getBytes()); // secret 长度应足够
        this.expireSeconds = expireSeconds;
    }

    /**
     * 生成 access token，subject 使用 user UUID 字符串
     */
    public String generateToken(UUID userUuid) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .setSubject(userUuid.toString())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token，返回用户 UUID 字符串
     */
    public String validateAndGetSubject(String token) throws JwtException {
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(hmacKey)
                .build()
                .parseClaimsJws(token);
        return claimsJws.getBody().getSubject();
    }
}
