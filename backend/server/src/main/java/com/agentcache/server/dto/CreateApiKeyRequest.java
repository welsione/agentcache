package com.agentcache.server.dto;

import com.agentcache.domain.enums.SpaceMemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建 API Key 请求。
 */
@Data
public class CreateApiKeyRequest {

    @NotBlank
    private String name;

    @NotNull
    private SpaceMemberRole role;
}