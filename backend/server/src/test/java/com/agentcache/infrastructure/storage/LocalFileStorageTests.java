package com.agentcache.infrastructure.storage;

import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.port.FileStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalFileStorage 测试。
 */
class LocalFileStorageTests {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreAndReadFile() throws IOException {
        FileStoragePort storage = new LocalFileStorage(tempDir.toString());
        String content = "hello agentcache";
        storage.store("space-1/file-1.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(),
                "text/plain");

        assertTrue(storage.exists("space-1/file-1.txt"));

        try (InputStream in = storage.read("space-1/file-1.txt")) {
            String read = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(content, read);
        }
    }

    @Test
    void shouldRejectPathTraversal() {
        FileStoragePort storage = new LocalFileStorage(tempDir.toString());
        assertThrows(ValidationException.class, () -> storage.store("../escape.txt",
                new ByteArrayInputStream("x".getBytes()), 1, "text/plain"));
        assertThrows(ValidationException.class, () -> storage.read("../escape.txt"));
    }
}
