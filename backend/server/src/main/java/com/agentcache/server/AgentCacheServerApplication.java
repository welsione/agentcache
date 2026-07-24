package com.agentcache.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AgentCache 后端启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.agentcache",
        exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableJpaRepositories(basePackages = "com.agentcache.domain.repository")
@EntityScan(basePackages = "com.agentcache.domain.entity")
@EnableScheduling
public class AgentCacheServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentCacheServerApplication.class, args);
    }
}