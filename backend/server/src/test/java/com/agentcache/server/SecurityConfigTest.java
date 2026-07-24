package com.agentcache.server;

import com.agentcache.server.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Security 配置加载测试。
 *
 * <p>使用 H2 内存数据库验证 SecurityConfig 与 JPA 扫描能正常初始化上下文。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain filterChain;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    void contextLoadsAndFilterChainConfigured() {
        assertThat(filterChain).isNotNull();
        assertThat(securityConfig).isNotNull();
    }
}