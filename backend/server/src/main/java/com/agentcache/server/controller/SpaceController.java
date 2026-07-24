package com.agentcache.server.controller;

import com.agentcache.application.dto.UpdateSpaceRequest;
import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.application.service.SpaceService;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.common.response.Result;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.server.dto.SpaceMemberRequest;
import com.agentcache.server.dto.SpaceMemberResponse;
import com.agentcache.server.dto.SpaceResponse;
import com.agentcache.server.dto.UpdateMemberRoleRequest;
import com.agentcache.server.security.AuthenticatedActor;
import com.agentcache.server.security.RequestActorResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 空间控制器。
 */
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final RequestActorResolver actorResolver;

    @GetMapping
    public Result<List<SpaceResponse>> list() {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(spaceService.listSpacesByUser(caller).stream()
                .map(SpaceResponse::from)
                .toList());
    }

    @PostMapping
    public Result<SpaceResponse> create(@Valid @RequestBody com.agentcache.application.dto.CreateSpaceRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(SpaceResponse.from(spaceService.createSpace(caller, request.getName(), request.getDescription())));
    }

    @GetMapping("/{id}")
    public Result<SpaceResponse> get(@PathVariable Long id) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(SpaceResponse.from(spaceService.getSpace(id, caller)));
    }

    /**
     * 更新空间信息。
     */
    @PutMapping("/{id}")
    public Result<SpaceResponse> update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateSpaceRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(SpaceResponse.from(spaceService.updateSpace(
                id, caller, request.getName(), request.getDescription(),
                request.getStorageType(), request.getDefaultVisibility())));
    }

    static AuthenticatedCaller toCaller(AuthenticatedActor actor) {
        return new AuthenticatedCaller(actor.getUserId(), actor.getKind() == AuthenticatedActor.Kind.API_KEY,
                actor.getSpaceId(), actor.getSpaceRole());
    }

    /**
     * 空间成员列表。
     */
    @GetMapping("/{id}/members")
    public Result<List<SpaceMemberResponse>> listMembers(@PathVariable Long id) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(spaceService.listMembers(id, caller).stream()
                .map(SpaceMemberResponse::from)
                .toList());
    }

    /**
     * 添加空间成员(MANAGER)。
     */
    @PostMapping("/{id}/members")
    public Result<SpaceMemberResponse> addMember(@PathVariable Long id,
                                                  @Valid @RequestBody SpaceMemberRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        SpaceMemberRole role = parseMemberRole(request.getRole());
        SpaceMember member = spaceService.addMember(id, request.getUserId(), role, caller);
        return Result.success(SpaceMemberResponse.from(member));
    }

    /**
     * 修改空间成员角色(MANAGER)。
     */
    @PutMapping("/{id}/members/{userId}")
    public Result<SpaceMemberResponse> changeMemberRole(@PathVariable Long id,
                                                        @PathVariable Long userId,
                                                        @Valid @RequestBody UpdateMemberRoleRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        SpaceMemberRole role = parseMemberRole(request.getRole());
        SpaceMember member = spaceService.changeMemberRole(id, userId, role, caller);
        return Result.success(SpaceMemberResponse.from(member));
    }

    /**
     * 移除空间成员(MANAGER)。
     */
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id,
                                     @PathVariable Long userId) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        spaceService.removeMember(id, userId, caller);
        return Result.success();
    }

    private SpaceMemberRole parseMemberRole(String value) {
        try {
            return SpaceMemberRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid space member role: " + value);
        }
    }
}
