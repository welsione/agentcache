package com.agentcache.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置属性。
 */
@Data
@ConfigurationProperties(prefix = "agentcache.jwt")
public class JwtProperties {

    private String secret;
    private Duration accessTokenTtl = Duration.ofHours(2);
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /**
     * 前端基础 URL，用于未认证浏览器请求重定向到登录页。
     */
    private String frontendBaseUrl = "http://localhost:5508";
}
