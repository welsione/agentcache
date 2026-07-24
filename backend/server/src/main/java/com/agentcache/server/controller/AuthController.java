package com.agentcache.server.controller;

import com.agentcache.application.service.AuthenticationService;
import com.agentcache.application.service.AuthenticationService.AuthenticatedUser;
import com.agentcache.application.service.InvitationService;
import com.agentcache.common.annotation.RequireAdmin;
import com.agentcache.common.response.Result;
import com.agentcache.domain.entity.InvitationToken;
import com.agentcache.server.dto.AcceptInvitationRequest;
import com.agentcache.server.dto.ChangePasswordRequest;
import com.agentcache.server.dto.CreateInvitationRequest;
import com.agentcache.server.dto.InvitationResponse;
import com.agentcache.server.dto.LoginRequest;
import com.agentcache.server.dto.LoginResponse;
import com.agentcache.server.security.AuthenticatedActor;
import com.agentcache.server.security.JwtTokenProvider;
import com.agentcache.server.security.RequestActorResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtTokenProvider tokenProvider;
    private final RequestActorResolver actorResolver;
    private final InvitationService invitationService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedUser user = authenticationService.authenticate(request.getUsername(), request.getPassword());
        String accessToken = tokenProvider.generateAccessToken(user.getUserId(), user.getUsername(), user.getRole());
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setMustChangePassword(user.getMustChangePassword());
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        return Result.success(response);
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedActor actor = actorResolver.require();
        authenticationService.changePassword(actor.getUserId(), request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    @PostMapping("/invitations")
    @RequireAdmin
    public Result<InvitationResponse> createInvitation(@RequestBody CreateInvitationRequest request) {
        AuthenticatedActor actor = actorResolver.require();
        InvitationToken invitation = invitationService.createInvitation(
                actor.getUserId(), request.getExpiresInHours());
        return Result.success(InvitationResponse.from(invitation));
    }

    @GetMapping("/invitations")
    @RequireAdmin
    public Result<Page<InvitationResponse>> listInvitations(Pageable pageable) {
        Page<InvitationResponse> page = invitationService.listInvitations(pageable)
                .map(InvitationResponse::from);
        return Result.success(page);
    }

    @DeleteMapping("/invitations/{id}")
    @RequireAdmin
    public Result<Void> revokeInvitation(@PathVariable Long id) {
        AuthenticatedActor actor = actorResolver.require();
        invitationService.revokeInvitation(id, actor.getUserId());
        return Result.success();
    }

    @PostMapping("/invite-accept")
    public Result<Void> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.acceptInvitation(
                request.getToken(), request.getUsername(), request.getEmail(), request.getPassword());
        return Result.success();
    }
}