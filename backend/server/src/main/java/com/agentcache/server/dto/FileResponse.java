package com.agentcache.server.dto;

import com.agentcache.domain.entity.FileRecord;
import com.agentcache.domain.enums.FileVisibility;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件响应 DTO。
 */
@Data
public class FileResponse {

    private Long id;
    private String originalName;
    private String contentType;
    private long size;
    private FileVisibility visibility;
    private String description;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private String accessUrl;

    /**
     * 实体转响应 DTO；accessUrl 基于可见性派生相对路径。
     */
    public static FileResponse from(FileRecord record) {
        FileResponse dto = new FileResponse();
        dto.id = record.getId();
        dto.originalName = record.getOriginalName();
        dto.contentType = record.getContentType();
        dto.size = record.getSize() == null ? 0L : record.getSize();
        dto.visibility = record.getVisibility();
        dto.description = record.getDescription();
        dto.expiresAt = record.getExpiresAt();
        dto.createdAt = record.getCreatedAt();
        dto.updatedAt = record.getUpdatedAt();
        dto.createdBy = record.getCreatedBy();
        dto.accessUrl = record.getVisibility() == FileVisibility.PUBLIC
                ? "/public/files/" + record.getId() + "/content"
                : "/api/files/" + record.getId() + "/content";
        return dto;
    }
}
