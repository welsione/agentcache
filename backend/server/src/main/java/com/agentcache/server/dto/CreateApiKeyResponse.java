package com.agentcache.server.dto;

import com.agentcache.domain.enums.SpaceMemberRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建 API Key 响应：仅在创建时返回明文 apiKey 一次。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyResponse {

    private Long id;
    private String apiKey;
    private String name;
    private SpaceMemberRole role;
    private LocalDateTime createdAt;
}