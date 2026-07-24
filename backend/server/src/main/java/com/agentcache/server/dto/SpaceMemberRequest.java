package com.agentcache.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 空间成员请求(添加成员)。
 */
@Data
public class SpaceMemberRequest {

    @NotNull
    private Long userId;

    @NotNull
    private String role;
}
