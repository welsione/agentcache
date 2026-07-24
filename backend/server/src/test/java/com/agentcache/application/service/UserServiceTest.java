package com.agentcache.application.service;

import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.entity.User;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.UserRole;
import com.agentcache.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserService 用户管理测试。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserThrowsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUser(99L));
    }

    @Test
    void changeRoleUpdatesUserRole() {
        User user = activeUser(UserRole.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.changeRole(1L, UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, updated.getRole());
        verify(repository).save(user);
    }

    @Test
    void changeRoleRejectsNull() {
        assertThrows(ValidationException.class, () -> userService.changeRole(1L, null));
    }

    @Test
    void changeStatusUpdatesUserStatus() {
        User user = activeUser(UserRole.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.changeStatus(1L, EntityStatus.DELETED);

        assertEquals(EntityStatus.DELETED, updated.getStatus());
    }

    @Test
    void deleteUserSoftDeletesByStatus() {
        User user = activeUser(UserRole.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteUser(1L);

        assertEquals(EntityStatus.DELETED, user.getStatus());
        verify(repository).save(user);
    }

    @Test
    void resetPasswordEncodesAndForcesChange() {
        User user = activeUser(UserRole.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass123")).thenReturn("hashed");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.resetPassword(1L, "newpass123");

        assertEquals("hashed", user.getPasswordHash());
        assertTrue(user.getMustChangePassword());
    }

    @Test
    void resetPasswordRejectsBlank() {
        assertThrows(ValidationException.class, () -> userService.resetPassword(1L, "  "));
    }

    @Test
    void resetPasswordRejectsShortPassword() {
        assertThrows(ValidationException.class, () -> userService.resetPassword(1L, "12345"));
    }

    private User activeUser(UserRole role) {
        return User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("old-hash")
                .role(role)
                .status(EntityStatus.ACTIVE)
                .mustChangePassword(false)
                .build();
    }
}
