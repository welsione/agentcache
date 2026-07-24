package com.agentcache.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户角色请求。
 */
@Data
public class UpdateRoleRequest {

    @NotNull
    private String role;
}
