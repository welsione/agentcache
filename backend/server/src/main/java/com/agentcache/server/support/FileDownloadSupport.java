package com.agentcache.server.support;

import com.agentcache.common.util.FilenameSanitizer;
import com.agentcache.domain.entity.FileRecord;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载响应构造工具。
 */
public final class FileDownloadSupport {

    private FileDownloadSupport() {
    }

    /**
     * 构造下载响应，自动处理 Content-Type 与文件名消毒。
     */
    public static ResponseEntity<InputStreamResource> build(FileRecord record, InputStream stream) {
        String safeName = FilenameSanitizer.sanitize(record.getOriginalName());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(safeName, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = resolveMediaType(record.getContentType());
        return ResponseEntity.ok()
                .header("Content-Disposition", disposition.toString())
                .contentType(mediaType)
                .body(new InputStreamResource(stream));
    }

    private static MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}