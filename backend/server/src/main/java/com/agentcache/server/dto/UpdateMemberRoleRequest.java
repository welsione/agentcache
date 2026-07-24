package com.agentcache.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改空间成员角色请求。
 */
@Data
public class UpdateMemberRoleRequest {

    @NotNull
    private String role;
}
