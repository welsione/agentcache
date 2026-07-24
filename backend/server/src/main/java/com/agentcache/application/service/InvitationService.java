package com.agentcache.application.service;

import com.agentcache.common.exception.AuthenticationException;
import com.agentcache.common.exception.DuplicateException;
import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.entity.InvitationToken;
import com.agentcache.domain.entity.User;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.UserRole;
import com.agentcache.domain.repository.InvitationTokenRepository;
import com.agentcache.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 邀请令牌应用服务。
 */
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationTokenRepository invitationTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建邀请令牌。
     *
     * @param adminUserId   创建者(ADMIN)用户 ID
     * @param expiresInHours 过期时间(小时),默认 72
     * @return 创建的邀请令牌
     */
    @Transactional
    public InvitationToken createInvitation(Long adminUserId, Integer expiresInHours) {
        if (expiresInHours == null || expiresInHours <= 0) {
            expiresInHours = 72;
        }
        if (expiresInHours > 720) {
            throw new ValidationException("Expiration time cannot exceed 720 hours");
        }
        String token = UUID.randomUUID().toString();
        InvitationToken invitation = InvitationToken.builder()
                .token(token)
                .createdBy(adminUserId)
                .expiresAt(TimeUtil.now().plusHours(expiresInHours))
                .build();
        return invitationTokenRepository.save(invitation);
    }

    /**
     * 查询邀请令牌列表(分页)。
     *
     * @param pageable 分页参数
     * @return 邀请令牌分页结果
     */
    public Page<InvitationToken> listInvitations(Pageable pageable) {
        return invitationTokenRepository.findAll(pageable);
    }

    /**
     * 撤销邀请令牌。
     *
     * @param invitationId 邀请令牌 ID
     * @param adminUserId  操作者(ADMIN)用户 ID
     */
    @Transactional
    public void revokeInvitation(Long invitationId, Long adminUserId) {
        InvitationToken invitation = invitationTokenRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
        if (invitation.getUsedAt() != null) {
            throw new ValidationException("Invitation already used");
        }
        // 将过期时间设为过去,使其失效
        invitation.setExpiresAt(TimeUtil.now().minusHours(1));
        invitationTokenRepository.save(invitation);
    }

    /**
     * 凭邀请令牌注册用户。
     *
     * @param token    邀请令牌
     * @param username 用户名
     * @param email    邮箱
     * @param password 密码
     * @return 创建的用户
     */
    @Transactional
    public User acceptInvitation(String token, String username, String email, String password) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Invitation token is required");
        }
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }

        InvitationToken invitation = invitationTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException("Invalid invitation token"));
        if (invitation.getUsedAt() != null) {
            throw new AuthenticationException("Invitation token already used");
        }
        if (invitation.getExpiresAt().isBefore(TimeUtil.now())) {
            throw new AuthenticationException("Invitation token expired");
        }

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateException("Email already exists: " + email);
        }

        User user = User.builder()
                .username(username.trim())
                .email(email.trim())
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.USER)
                .status(EntityStatus.ACTIVE)
                .mustChangePassword(true)
                .build();
        user = userRepository.save(user);

        // 标记令牌已使用
        invitation.setUsedAt(TimeUtil.now());
        invitationTokenRepository.save(invitation);

        return user;
    }
}
