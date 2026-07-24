package com.agentcache.application.service;

import com.agentcache.application.service.ApiKeyService.ApiKeyValidation;
import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.application.service.ApiKeyService.CreateApiKeyOutcome;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.entity.ApiKey;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.repository.ApiKeyRepository;
import com.agentcache.domain.repository.SpaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ApiKeyService 角色约束测试。
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private SpaceMemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private AuthenticatedCaller user(long userId) {
        return new AuthenticatedCaller(userId, false, null, null);
    }

    @Test
    void readerCannotListApiKeys() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.READER).build()));

        assertThrows(UnauthorizedException.class, () -> apiKeyService.listBySpace(1L, user(10L)));
    }

    @Test
    void memberCannotCreateApiKeys() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.MEMBER).build()));

        assertThrows(UnauthorizedException.class,
                () -> apiKeyService.createApiKey(1L, user(10L), "ci", SpaceMemberRole.READER));
    }

    @Test
    void managerCannotRequestHigherRoleThanSelf() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.MEMBER).build()));

        assertThrows(UnauthorizedException.class,
                () -> apiKeyService.createApiKey(1L, user(10L), "ci", SpaceMemberRole.MANAGER));
    }

    @Test
    void managerCanCreateApiKeyWithLowerRole() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.MANAGER).build()));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            k.setId(42L);
            return k;
        });

        CreateApiKeyOutcome outcome = apiKeyService.createApiKey(1L, user(10L), "ci", SpaceMemberRole.READER);
        assertNotNull(outcome.plainKey());
        assertEquals(42L, outcome.apiKey().getId());
    }

    @Test
    void blankNameRejected() {
        when(memberRepository.findBySpaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(SpaceMember.builder().spaceId(1L).userId(10L).role(SpaceMemberRole.MANAGER).build()));

        assertThrows(ValidationException.class,
                () -> apiKeyService.createApiKey(1L, user(10L), "  ", SpaceMemberRole.READER));
    }

    @Test
    void invalidApiKeyRejected() {
        assertThrows(UnauthorizedException.class, () -> apiKeyService.validateApiKey("not-prefixed"));
        assertThrows(UnauthorizedException.class, () -> apiKeyService.validateApiKey(null));
    }

    @Test
    void validApiKeyAuthenticated() {
        when(apiKeyRepository.findByKeyPrefixAndStatus(anyString(), eq(EntityStatus.ACTIVE)))
                .thenReturn(List.of(ApiKey.builder().id(1L).spaceId(2L).role(SpaceMemberRole.MANAGER)
                        .keyHash("hash").status(EntityStatus.ACTIVE).build()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyValidation v = apiKeyService.validateApiKey("ak-abcdefghijklmnop");
        assertEquals(1L, v.apiKeyId());
        assertEquals(2L, v.spaceId());
        assertEquals(SpaceMemberRole.MANAGER, v.role());
    }
}