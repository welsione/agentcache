package com.agentcache.infrastructure.storage;

import com.agentcache.domain.enums.StorageType;
import com.agentcache.domain.port.FileStoragePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 存储自动配置。
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    public Map<StorageType, FileStoragePort> fileStoragePorts(StorageProperties properties) {
        LocalFileStorage local = new LocalFileStorage(properties.getLocal().getBasePath());
        // COS 实现暂未开发，后续添加
        return Map.of(StorageType.LOCAL, local);
    }
}
