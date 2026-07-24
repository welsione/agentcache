package com.agentcache.server.controller;

import com.agentcache.common.annotation.RequireAdmin;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.UserRole;
import com.agentcache.application.service.UserService;
import com.agentcache.common.response.Result;
import com.agentcache.domain.entity.User;
import com.agentcache.server.dto.ResetPasswordRequest;
import com.agentcache.server.dto.UpdateRoleRequest;
import com.agentcache.server.dto.UpdateStatusRequest;
import com.agentcache.server.dto.UserResponse;
import com.agentcache.server.security.AuthenticatedActor;
import com.agentcache.server.security.RequestActorResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RequestActorResolver actorResolver;

    /**
     * 获取当前用户信息。
     */
    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser() {
        AuthenticatedActor actor = actorResolver.require();
        User user = userService.getUser(actor.getUserId());
        return Result.success(UserResponse.from(user));
    }

    /**
     * 用户列表(ADMIN)。
     */
    @GetMapping
    @RequireAdmin
    public Result<Page<UserResponse>> listUsers(Pageable pageable) {
        Page<UserResponse> page = userService.listUsers(pageable).map(UserResponse::from);
        return Result.success(page);
    }

    /**
     * 修改用户全局角色(ADMIN)。
     */
    @PutMapping("/{id}/role")
    @RequireAdmin
    public Result<UserResponse> changeRole(@PathVariable Long id,
                                           @Valid @RequestBody UpdateRoleRequest request) {
        UserRole role = parseUserRole(request.getRole());
        User user = userService.changeRole(id, role);
        return Result.success(UserResponse.from(user));
    }

    /**
     * 修改用户状态(ADMIN)。
     */
    @PutMapping("/{id}/status")
    @RequireAdmin
    public Result<UserResponse> changeStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateStatusRequest request) {
        EntityStatus status = parseEntityStatus(request.getStatus());
        User user = userService.changeStatus(id, status);
        return Result.success(UserResponse.from(user));
    }

    /**
     * 重置用户密码(ADMIN)。
     */
    @PutMapping("/{id}/password")
    @RequireAdmin
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    /**
     * 删除用户(软删除,ADMIN)。
     */
    @DeleteMapping("/{id}")
    @RequireAdmin
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    private UserRole parseUserRole(String value) {
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role: " + value);
        }
    }

    private EntityStatus parseEntityStatus(String value) {
        try {
            return EntityStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + value);
        }
    }
}
