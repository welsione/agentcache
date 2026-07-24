package com.agentcache.application.service;

import com.agentcache.common.exception.AuthenticationException;
import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.entity.User;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证领域服务：校验用户名密码并颁发访问令牌所需的身份信息。
 *
 * <p>未知用户与错误密码统一抛出 {@link AuthenticationException}，避免泄露账户存在性。</p>
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 校验凭据并返回认证后的用户。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 已校验的用户
     * @throws AuthenticationException 当用户不存在、密码错误或账户非激活状态
     */
    public AuthenticatedUser authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Invalid credentials");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
        if (user.getStatus() != EntityStatus.ACTIVE) {
            throw new AuthenticationException("Invalid credentials");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole().name(), user.getMustChangePassword());
    }

    /**
     * 修改当前用户密码。
     *
     * @param userId      用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("Old password and new password are required");
        }
        if (newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Old password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    /** 已认证的用户信息。 */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthenticatedUser {
        private Long userId;
        private String username;
        private String role;
        private Boolean mustChangePassword;
    }
}