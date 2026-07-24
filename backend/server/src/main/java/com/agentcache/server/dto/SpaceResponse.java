package com.agentcache.server.dto;

import com.agentcache.domain.entity.Space;
import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.StorageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间响应 DTO。
 */
@Data
public class SpaceResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private StorageType storageType;
    private FileVisibility defaultVisibility;
    private LocalDateTime createdAt;

    public static SpaceResponse from(Space space) {
        SpaceResponse dto = new SpaceResponse();
        dto.id = space.getId();
        dto.name = space.getName();
        dto.description = space.getDescription();
        dto.ownerId = space.getOwnerId();
        dto.storageType = space.getStorageType();
        dto.defaultVisibility = space.getDefaultVisibility();
        dto.createdAt = space.getCreatedAt();
        return dto;
    }
}