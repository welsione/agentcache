package com.agentcache.application.service;

import com.agentcache.common.exception.DuplicateException;
import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.entity.User;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.UserRole;
import com.agentcache.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户信息。
     */
    public User getUser(Long userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * 用户列表(分页)。
     */
    public Page<User> listUsers(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * 修改用户全局角色。
     */
    @Transactional
    public User changeRole(Long userId, UserRole role) {
        if (role == null) {
            throw new ValidationException("Role is required");
        }
        User user = getUser(userId);
        user.setRole(role);
        return repository.save(user);
    }

    /**
     * 修改用户状态(启用/禁用)。
     */
    @Transactional
    public User changeStatus(Long userId, EntityStatus status) {
        if (status == null) {
            throw new ValidationException("Status is required");
        }
        User user = getUser(userId);
        user.setStatus(status);
        return repository.save(user);
    }

    /**
     * 删除用户(软删除)。
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUser(userId);
        user.setStatus(EntityStatus.DELETED);
        repository.save(user);
    }

    /**
     * 重置用户密码(ADMIN 操作,重置后用户需强制改密)。
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password is required");
        }
        if (newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters");
        }
        User user = getUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        repository.save(user);
    }

    /**
     * 创建管理员(保留,供初始化使用)。
     */
    @Transactional
    public User createAdmin(String username, String email, String rawPassword) {
        if (repository.existsByUsername(username)) {
            throw new DuplicateException("Username already exists: " + username);
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(UserRole.ADMIN)
                .build();
        return repository.save(user);
    }

    /**
     * 按用户名查找。
     */
    public User findByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
