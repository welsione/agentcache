package com.agentcache.cli.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件响应 DTO（镜像后端 {@code FileResponse}）。
 */
@Data
public class FileResponse {

    private Long id;
    private String originalName;
    private String contentType;
    private long size;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private String accessUrl;
}