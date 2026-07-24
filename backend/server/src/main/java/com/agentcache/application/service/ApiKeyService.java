package com.agentcache.application.service;

import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.entity.ApiKey;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.repository.ApiKeyRepository;
import com.agentcache.domain.repository.SpaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * API Key 应用服务。
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "ak-";
    private final ApiKeyRepository apiKeyRepository;
    private final SpaceMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<ApiKey> listBySpace(Long spaceId, AuthenticatedCaller caller) {
        requireManager(caller, spaceId);
        return apiKeyRepository.findBySpaceIdAndStatus(spaceId, EntityStatus.ACTIVE);
    }

    @Transactional
    public CreateApiKeyOutcome createApiKey(Long spaceId, AuthenticatedCaller caller, String name, SpaceMemberRole role) {
        SpaceMemberRole callerRole = requireManager(caller, spaceId);
        if (role == null) {
            throw new ValidationException("API Key role is required");
        }
        if (!isAtLeast(callerRole, role)) {
            throw new UnauthorizedException(
                    "Requested role " + role + " exceeds caller role " + callerRole);
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("API Key name is required");
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String plainKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String keyPrefix = extractPrefix(plainKey);
        String keyHash = passwordEncoder.encode(plainKey);

        ApiKey apiKey = ApiKey.builder()
                .spaceId(spaceId)
                .name(name)
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .role(role)
                .createdBy(caller.userId())
                .build();
        apiKey = apiKeyRepository.save(apiKey);
        return new CreateApiKeyOutcome(apiKey, plainKey);
    }

    @Transactional
    public void revokeApiKey(Long spaceId, Long keyId, AuthenticatedCaller caller) {
        requireManager(caller, spaceId);
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key not found: " + keyId));
        if (!key.getSpaceId().equals(spaceId)) {
            throw new UnauthorizedException("API Key does not belong to this space");
        }
        key.setStatus(EntityStatus.DELETED);
        apiKeyRepository.save(key);
    }

    @Transactional
    public ApiKeyValidation validateApiKey(String plainKey) {
        if (plainKey == null || !plainKey.startsWith(KEY_PREFIX)) {
            throw new UnauthorizedException("Invalid API Key");
        }
        String keyPrefix = extractPrefix(plainKey);
        List<ApiKey> candidates = apiKeyRepository.findByKeyPrefixAndStatus(keyPrefix, EntityStatus.ACTIVE);
        for (ApiKey key : candidates) {
            if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(TimeUtil.now())) {
                continue;
            }
            if (passwordEncoder.matches(plainKey, key.getKeyHash())) {
                key.setLastUsedAt(TimeUtil.now());
                apiKeyRepository.save(key);
                return new ApiKeyValidation(
                        key.getId(),
                        key.getSpaceId(),
                        key.getRole());
            }
        }
        throw new UnauthorizedException("Invalid API Key");
    }

    /**
     * 检查调用者在目标空间中的角色是否为 MANAGER，否则抛出异常。
     *
     * @return 调用者在空间中的实际角色
     */
    private SpaceMemberRole requireManager(AuthenticatedCaller caller, Long spaceId) {
        if (caller == null) {
            throw new UnauthorizedException("User authentication required");
        }
        if (caller.isApiKey()) {
            if (caller.spaceId() == null || !caller.spaceId().equals(spaceId)) {
                throw new UnauthorizedException("API Key not bound to space: " + spaceId);
            }
            if (caller.spaceRole() != SpaceMemberRole.MANAGER) {
                throw new UnauthorizedException("API Key role cannot manage API keys");
            }
            return caller.spaceRole();
        }
        if (caller.userId() == null) {
            throw new UnauthorizedException("User authentication required");
        }
        SpaceMember member = memberRepository.findBySpaceIdAndUserId(spaceId, caller.userId())
                .orElseThrow(() -> new UnauthorizedException("No access to space: " + spaceId));
        if (member.getRole() != SpaceMemberRole.MANAGER) {
            throw new UnauthorizedException("Only managers can manage API keys");
        }
        return member.getRole();
    }

    private boolean isAtLeast(SpaceMemberRole caller, SpaceMemberRole requested) {
        return rank(caller) >= rank(requested);
    }

    private int rank(SpaceMemberRole role) {
        return switch (role) {
            case MANAGER -> 2;
            case MEMBER -> 1;
            case READER -> 0;
        };
    }

    private String extractPrefix(String plainKey) {
        String payload = plainKey.startsWith(KEY_PREFIX) ? plainKey.substring(KEY_PREFIX.length()) : plainKey;
        return payload.length() > 8 ? payload.substring(0, 8) : payload;
    }

    /** 调用者上下文，便于服务层在不依赖 Web 模块类型的前提下表达用户/API Key 身份。 */
    public record AuthenticatedCaller(Long userId, boolean apiKey, Long spaceId, SpaceMemberRole spaceRole) {
        public boolean isApiKey() {
            return apiKey;
        }
    }

    /** API Key 校验结果。 */
    public record ApiKeyValidation(Long apiKeyId, Long spaceId, SpaceMemberRole role) {
    }

    /** API Key 创建结果，包含持久化实体与明文（仅创建时返回一次）。 */
    public record CreateApiKeyOutcome(ApiKey apiKey, String plainKey) {
    }
}