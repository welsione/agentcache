package com.agentcache.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重置密码请求(ADMIN 操作)。
 */
@Data
public class ResetPasswordRequest {

    @NotBlank
    private String newPassword;
}
