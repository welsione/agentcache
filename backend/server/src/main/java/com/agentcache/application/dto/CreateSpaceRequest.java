package com.agentcache.application.dto;

import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.StorageType;
import lombok.Data;

/**
 * 创建空间请求。
 */
@Data
public class CreateSpaceRequest {

    private String name;
    private String description;
    private StorageType storageType;
    private FileVisibility defaultVisibility;
}
