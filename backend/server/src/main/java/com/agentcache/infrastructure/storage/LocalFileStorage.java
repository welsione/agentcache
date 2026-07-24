package com.agentcache.infrastructure.storage;

import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.port.FileStoragePort;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 本地文件存储实现。
 */
@Slf4j
public class LocalFileStorage implements FileStoragePort {

    private final String basePath;

    public LocalFileStorage(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public StorageType type() {
        return StorageType.LOCAL;
    }

    @Override
    public void store(String path, InputStream content, long size, String contentType) {
        Path target = resolve(path);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {}", target);
        } catch (IOException e) {
            throw new ValidationException("Failed to store file: " + path, e);
        }
    }

    @Override
    public InputStream read(String path) {
        Path target = resolve(path);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new ValidationException("Failed to read file: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        Path target = resolve(path);
        try {
            Files.deleteIfExists(target);
            log.info("Deleted file: {}", target);
        } catch (IOException e) {
            throw new ValidationException("Failed to delete file: " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(resolve(path));
    }

    @Override
    public String getUrl(String path, Duration expiry) {
        return "/api/files/{id}/content";
    }

    private Path resolve(String path) {
        if (path == null || path.isBlank() || path.contains("..") || path.contains("\0")) {
            throw new ValidationException("Invalid storage path: " + path);
        }
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path target = base.resolve(path).normalize();
        if (!target.startsWith(base)) {
            throw new ValidationException("Storage path escapes base directory: " + path);
        }
        return target;
    }
}
