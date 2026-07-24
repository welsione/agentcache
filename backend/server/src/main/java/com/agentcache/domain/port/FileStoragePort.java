package com.agentcache.domain.port;

import java.io.InputStream;
import java.time.Duration;

/**
 * 文件存储端口。
 */
public interface FileStoragePort {

    StorageType type();

    void store(String path, InputStream content, long size, String contentType);

    InputStream read(String path);

    void delete(String path);

    boolean exists(String path);

    String getUrl(String path, Duration expiry);

    enum StorageType {
        LOCAL,
        COS
    }
}
