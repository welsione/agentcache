package com.agentcache.application.service;

import com.agentcache.common.exception.AuthenticationException;
import com.agentcache.common.exception.DuplicateException;
import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.entity.InvitationToken;
import com.agentcache.domain.entity.User;
import com.agentcache.domain.repository.InvitationTokenRepository;
import com.agentcache.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InvitationService 邀请令牌流程测试。
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationTokenRepository invitationTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void createInvitationUsesDefaultTtlWhenNull() {
        when(invitationTokenRepository.save(any(InvitationToken.class))).thenAnswer(inv -> {
            InvitationToken t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        InvitationToken token = invitationService.createInvitation(7L, null);

        ArgumentCaptor<InvitationToken> captor = ArgumentCaptor.forClass(InvitationToken.class);
        verify(invitationTokenRepository).save(captor.capture());
        InvitationToken saved = captor.getValue();
        assertEquals(7L, saved.getCreatedBy());
        assertNotNull(saved.getToken());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusHours(71)));
        assertEquals(token.getId(), 1L);
    }

    @Test
    void createInvitationRejectsExcessiveTtl() {
        assertThrows(ValidationException.class, () -> invitationService.createInvitation(7L, 1000));
    }

    @Test
    void revokeInvitationFailsWhenAlreadyUsed() {
        InvitationToken token = InvitationToken.builder()
                .id(1L).token("t").createdBy(7L).usedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(invitationTokenRepository.findById(1L)).thenReturn(Optional.of(token));

        assertThrows(ValidationException.class, () -> invitationService.revokeInvitation(1L, 7L));
    }

    @Test
    void revokeInvitationFailsWhenNotFound() {
        when(invitationTokenRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invitationService.revokeInvitation(99L, 7L));
    }

    @Test
    void acceptInvitationRejectsBlankToken() {
        assertThrows(ValidationException.class,
                () -> invitationService.acceptInvitation("", "bob", "bob@example.com", "secret123"));
    }

    @Test
    void acceptInvitationRejectsShortPassword() {
        assertThrows(ValidationException.class,
                () -> invitationService.acceptInvitation("tok", "bob", "bob@example.com", "12345"));
    }

    @Test
    void acceptInvitationFailsOnUnknownToken() {
        when(invitationTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());
        assertThrows(AuthenticationException.class,
                () -> invitationService.acceptInvitation("ghost", "bob", "bob@example.com", "secret123"));
    }

    @Test
    void acceptInvitationFailsOnUsedToken() {
        InvitationToken token = InvitationToken.builder()
                .token("used").usedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(invitationTokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThrows(AuthenticationException.class,
                () -> invitationService.acceptInvitation("used", "bob", "bob@example.com", "secret123"));
    }

    @Test
    void acceptInvitationFailsOnExpiredToken() {
        InvitationToken token = InvitationToken.builder()
                .token("expired").expiresAt(LocalDateTime.now().minusHours(1)).build();
        when(invitationTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThrows(AuthenticationException.class,
                () -> invitationService.acceptInvitation("expired", "bob", "bob@example.com", "secret123"));
    }

    @Test
    void acceptInvitationFailsOnDuplicateUsername() {
        InvitationToken token = validToken();
        when(invitationTokenRepository.findByToken("ok")).thenReturn(Optional.of(token));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThrows(DuplicateException.class,
                () -> invitationService.acceptInvitation("ok", "bob", "bob@example.com", "secret123"));
    }

    @Test
    void acceptInvitationCreatesUserAndMarksTokenUsed() {
        InvitationToken token = validToken();
        when(invitationTokenRepository.findByToken("ok")).thenReturn(Optional.of(token));
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(invitationTokenRepository.save(any(InvitationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = invitationService.acceptInvitation("ok", "bob", "bob@example.com", "secret123");

        assertEquals(42L, user.getId());
        assertEquals("bob", user.getUsername());
        assertEquals("hashed", user.getPasswordHash());
        assertTrue(user.getMustChangePassword());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(any(User.class));
        verify(invitationTokenRepository).save(token);
    }

    private InvitationToken validToken() {
        return InvitationToken.builder()
                .token("ok").createdBy(7L)
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
    }
}
