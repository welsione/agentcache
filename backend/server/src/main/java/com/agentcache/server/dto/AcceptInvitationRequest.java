package com.agentcache.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 接受邀请请求。
 */
@Data
public class AcceptInvitationRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
