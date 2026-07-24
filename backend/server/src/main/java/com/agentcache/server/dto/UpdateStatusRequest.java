package com.agentcache.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户状态请求。
 */
@Data
public class UpdateStatusRequest {

    @NotNull
    private String status;
}
