package com.agentcache.server.security;

import com.agentcache.server.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 生成与解析。
 *
 * <p>访问令牌中包含 {@code userId}、{@code username}、{@code role} 三个核心 claim。
 * 短时效令牌下，role/status 在过期前不会自动失效；如果需要即时吊销，需要额外的撤销列表
 * （不在 MVP 范围内）。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties properties;

    @PostConstruct
    void validateSecret() {
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set the JWT_SECRET environment variable (>=32 bytes).");
        }
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes (256 bits) for HS256. Got " + bytes.length + " bytes.");
        }
    }

    private SecretKey key() {
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌。
     *
     * @param userId   用户 ID，写入 subject
     * @param username 用户名
     * @param role     用户全局角色
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getAccessTokenTtl().toMillis());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}