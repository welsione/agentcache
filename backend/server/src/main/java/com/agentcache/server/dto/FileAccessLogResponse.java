package com.agentcache.server.dto;

import com.agentcache.domain.entity.FileAccessLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件访问日志响应 DTO。
 */
@Data
public class FileAccessLogResponse {

    private Long id;
    private Long fileId;
    private Long spaceId;
    private String action;
    private String actorType;
    private Long actorId;
    private String actorName;
    private String ip;
    private String details;
    private LocalDateTime createdAt;

    /**
     * 实体转响应 DTO。
     */
    public static FileAccessLogResponse from(FileAccessLog log) {
        FileAccessLogResponse dto = new FileAccessLogResponse();
        dto.id = log.getId();
        dto.fileId = log.getFileId();
        dto.spaceId = log.getSpaceId();
        dto.action = log.getAction().name();
        dto.actorType = log.getActorType().name();
        dto.actorId = log.getActorId();
        dto.actorName = log.getActorName();
        dto.ip = log.getIp();
        dto.details = log.getDetails();
        dto.createdAt = log.getCreatedAt();
        return dto;
    }
}
