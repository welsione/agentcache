package com.agentcache.server.dto;

import com.agentcache.domain.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应。
 */
@Data
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private Boolean mustChangePassword;
    private LocalDateTime createdAt;

    /**
     * 从实体转换为响应 DTO。
     */
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setStatus(user.getStatus().name());
        response.setMustChangePassword(user.getMustChangePassword());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
