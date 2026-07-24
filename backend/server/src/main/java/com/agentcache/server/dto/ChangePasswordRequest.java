package com.agentcache.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求。
 */
@Data
public class ChangePasswordRequest {

    @NotBlank
    private String oldPassword;

    @NotBlank
    private String newPassword;
}
