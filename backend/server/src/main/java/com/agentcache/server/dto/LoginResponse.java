package com.agentcache.server.dto;

import lombok.Data;

/**
 * 登录响应。
 */
@Data
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Boolean mustChangePassword;
    private Long userId;
    private String username;
    private String role;
}
