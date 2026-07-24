package com.agentcache.application.service;

import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.common.exception.DuplicateException;
import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.domain.entity.Space;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.enums.StorageType;
import com.agentcache.domain.repository.SpaceMemberRepository;
import com.agentcache.domain.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 空间应用服务。
 */
@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository memberRepository;

    @Transactional
    public Space createSpace(AuthenticatedCaller caller, String name, String description) {
        if (caller == null || caller.userId() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Space name is required");
        }
        Space space = Space.builder()
                .name(name.trim())
                .description(description)
                .ownerId(caller.userId())
                .build();
        space = spaceRepository.save(space);

        SpaceMember member = SpaceMember.builder()
                .spaceId(space.getId())
                .userId(caller.userId())
                .role(SpaceMemberRole.MANAGER)
                .build();
        memberRepository.save(member);
        return space;
    }

    public List<Space> listSpacesByUser(AuthenticatedCaller caller) {
        if (caller == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (caller.isApiKey()) {
            if (caller.spaceId() == null) {
                throw new UnauthorizedException("API Key not bound to space");
            }
            return spaceRepository.findAllById(List.of(caller.spaceId()));
        }
        if (caller.userId() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        List<Long> spaceIds = memberRepository.findByUserId(caller.userId()).stream()
                .map(SpaceMember::getSpaceId)
                .toList();
        return spaceRepository.findAllById(spaceIds);
    }

    public Space getSpace(Long spaceId, AuthenticatedCaller caller) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + spaceId));
        if (!memberRepository.existsBySpaceIdAndUserId(spaceId, caller.userId())) {
            throw new UnauthorizedException("No access to space: " + spaceId);
        }
        return space;
    }

    /**
     * 获取空间成员列表。
     *
     * @param spaceId 空间 ID
     * @param caller  调用者
     * @return 空间成员列表
     */
    public List<SpaceMember> listMembers(Long spaceId, AuthenticatedCaller caller) {
        requireSpaceAccess(spaceId, caller);
        return memberRepository.findBySpaceId(spaceId);
    }

    /**
     * 添加空间成员。
     *
     * @param spaceId 空间 ID
     * @param userId  用户 ID
     * @param role    空间角色
     * @param caller  调用者(需 MANAGER)
     */
    @Transactional
    public SpaceMember addMember(Long spaceId, Long userId, SpaceMemberRole role, AuthenticatedCaller caller) {
        requireManager(caller, spaceId);
        if (userId == null) {
            throw new ValidationException("User ID is required");
        }
        if (role == null) {
            throw new ValidationException("Role is required");
        }
        if (memberRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw new DuplicateException("User is already a member of this space");
        }
        SpaceMember member = SpaceMember.builder()
                .spaceId(spaceId)
                .userId(userId)
                .role(role)
                .build();
        return memberRepository.save(member);
    }

    /**
     * 修改空间成员角色。
     *
     * @param spaceId 空间 ID
     * @param userId  用户 ID
     * @param role    新角色
     * @param caller  调用者(需 MANAGER)
     */
    @Transactional
    public SpaceMember changeMemberRole(Long spaceId, Long userId, SpaceMemberRole role, AuthenticatedCaller caller) {
        requireManager(caller, spaceId);
        if (role == null) {
            throw new ValidationException("Role is required");
        }
        SpaceMember member = memberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in space"));
        member.setRole(role);
        return memberRepository.save(member);
    }

    /**
     * 移除空间成员。
     *
     * @param spaceId 空间 ID
     * @param userId  用户 ID
     * @param caller  调用者(需 MANAGER)
     */
    @Transactional
    public void removeMember(Long spaceId, Long userId, AuthenticatedCaller caller) {
        requireManager(caller, spaceId);
        if (!memberRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw new ResourceNotFoundException("Member not found in space");
        }
        memberRepository.deleteBySpaceIdAndUserId(spaceId, userId);
    }

    /**
     * 更新空间信息。
     */
    @Transactional
    public Space updateSpace(Long spaceId, AuthenticatedCaller caller, String name,
                              String description, StorageType storageType,
                              FileVisibility defaultVisibility) {
        requireManager(caller, spaceId);
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + spaceId));
        if (name != null && !name.isBlank()) {
            space.setName(name.trim());
        }
        if (description != null) {
            space.setDescription(description);
        }
        if (storageType != null) {
            space.setStorageType(storageType);
        }
        if (defaultVisibility != null) {
            space.setDefaultVisibility(defaultVisibility);
        }
        return spaceRepository.save(space);
    }

    /**
     * 校验调用者有空间访问权限。
     */
    private void requireSpaceAccess(Long spaceId, AuthenticatedCaller caller) {
        if (caller == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (caller.isApiKey()) {
            if (caller.spaceId() == null || !caller.spaceId().equals(spaceId)) {
                throw new UnauthorizedException("No access to space: " + spaceId);
            }
            return;
        }
        // ADMIN 用户可访问所有空间
        if (!memberRepository.existsBySpaceIdAndUserId(spaceId, caller.userId())) {
            // 还需检查用户是否是 ADMIN(ADMIN 可访问所有空间)
            // 此处简化: 非成员则拒绝,ADMIN 通过 AOP @RequireAdmin 旁路
            throw new UnauthorizedException("No access to space: " + spaceId);
        }
    }

    /**
     * 校验调用者是空间 MANAGER。
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
                throw new UnauthorizedException("Manager role required");
            }
            return caller.spaceRole();
        }
        SpaceMember member = memberRepository.findBySpaceIdAndUserId(spaceId, caller.userId())
                .orElseThrow(() -> new UnauthorizedException("No access to space: " + spaceId));
        if (member.getRole() != SpaceMemberRole.MANAGER) {
            throw new UnauthorizedException("Only managers can manage members");
        }
        return member.getRole();
    }
}