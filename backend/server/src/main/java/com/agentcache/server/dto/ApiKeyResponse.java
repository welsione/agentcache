package com.agentcache.server.dto;

import com.agentcache.domain.entity.ApiKey;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.SpaceMemberRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API Key 列表响应 DTO；不暴露 keyHash 与 keyPrefix。
 */
@Data
public class ApiKeyResponse {

    private Long id;
    private Long spaceId;
    private String name;
    private SpaceMemberRole role;
    private EntityStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;

    public static ApiKeyResponse from(ApiKey apiKey) {
        ApiKeyResponse dto = new ApiKeyResponse();
        dto.id = apiKey.getId();
        dto.spaceId = apiKey.getSpaceId();
        dto.name = apiKey.getName();
        dto.role = apiKey.getRole();
        dto.status = apiKey.getStatus();
        dto.createdAt = apiKey.getCreatedAt();
        dto.lastUsedAt = apiKey.getLastUsedAt();
        dto.expiresAt = apiKey.getExpiresAt();
        return dto;
    }
}