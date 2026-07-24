package com.agentcache.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性。
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String type = "local";
    private Local local = new Local();

    @Data
    public static class Local {
        private String basePath = "/data/files";
    }
}
