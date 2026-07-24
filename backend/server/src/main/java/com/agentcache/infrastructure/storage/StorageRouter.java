package com.agentcache.infrastructure.storage;

import com.agentcache.domain.enums.StorageType;
import com.agentcache.domain.port.FileStoragePort;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 存储路由器，根据存储类型选择对应的 FileStoragePort 实现。
 */
@Component
public class StorageRouter {

    private final Map<StorageType, FileStoragePort> ports;

    public StorageRouter(Map<StorageType, FileStoragePort> ports) {
        this.ports = ports;
    }

    /**
     * 根据存储类型获取对应的存储实现。
     *
     * @param type 存储类型
     * @return 对应的 FileStoragePort 实现
     */
    public FileStoragePort resolve(StorageType type) {
        FileStoragePort port = ports.get(type);
        if (port == null) {
            throw new IllegalArgumentException("Unsupported storage type: " + type);
        }
        return port;
    }
}
