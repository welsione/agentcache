package com.agentcache.application.dto;

import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.StorageType;
import lombok.Data;

/**
 * 更新空间请求 DTO。
 */
@Data
public class UpdateSpaceRequest {

    private String name;
    private String description;
    private StorageType storageType;
    private FileVisibility defaultVisibility;
}
