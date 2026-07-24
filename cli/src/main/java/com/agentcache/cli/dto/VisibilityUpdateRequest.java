package com.agentcache.cli.dto;

import lombok.Data;

/**
 * 可见性更新请求体。
 */
@Data
public class VisibilityUpdateRequest {

    private String visibility;
}