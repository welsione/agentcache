package com.agentcache.server.dto;

import com.agentcache.domain.enums.FileVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件可见性更新请求。
 */
@Data
public class VisibilityUpdateRequest {

    @NotNull
    private FileVisibility visibility;
}
