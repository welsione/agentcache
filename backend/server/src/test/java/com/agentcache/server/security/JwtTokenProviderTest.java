package com.agentcache.server.security;

import com.agentcache.server.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 测试。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider providerWithSecret(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        provider.validateSecret();
        return provider;
    }

    @Test
    void shortSecretRejected() {
        assertThatThrownBy(() -> providerWithSecret("tooshort"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void nullSecretRejected() {
        assertThatThrownBy(() -> providerWithSecret(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generatedTokenContainsClaims() {
        JwtTokenProvider provider = providerWithSecret("a-sufficient-secret-of-at-least-32-bytes-long");
        String token = provider.generateAccessToken(42L, "alice", "ADMIN");
        Claims claims = provider.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }
}