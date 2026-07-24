package com.agentcache.server.security;

import com.agentcache.common.annotation.RequireAdmin;
import com.agentcache.common.annotation.RequireSpaceRole;
import com.agentcache.common.annotation.SpaceId;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.domain.enums.SpaceMemberRole;
import com.agentcache.domain.entity.SpaceMember;
import com.agentcache.domain.repository.SpaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * 权限校验 AOP 切面。
 *
 * <p>拦截标注了 {@link RequireAdmin} 或 {@link RequireSpaceRole} 的方法,
 * 从 {@link AuthenticatedActor} 校验权限。</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuthorizationAspect {

    private final RequestActorResolver actorResolver;
    private final SpaceMemberRepository spaceMemberRepository;

    /**
     * 校验 ADMIN 全局角色。
     */
    @Before("@annotation(com.agentcache.common.annotation.RequireAdmin)")
    public void checkAdmin(JoinPoint joinPoint) {
        AuthenticatedActor actor = actorResolver.require();
        if (!"ADMIN".equals(actor.getUserRole())) {
            throw new UnauthorizedException("Admin role required");
        }
    }

    /**
     * 校验空间角色。
     */
    @Before("@annotation(requireSpaceRole)")
    public void checkSpaceRole(JoinPoint joinPoint, RequireSpaceRole requireSpaceRole) {
        AuthenticatedActor actor = actorResolver.require();
        Long spaceId = resolveSpaceId(joinPoint);
        if (spaceId == null) {
            throw new UnauthorizedException("Space ID not found in method parameters");
        }
        SpaceMemberRole required = requireSpaceRole.value();
        SpaceMemberRole actual = resolveSpaceRole(actor, spaceId);
        if (rank(actual) < rank(required)) {
            throw new UnauthorizedException("Required space role: " + required + ", actual: " + actual);
        }
    }

    /**
     * 解析方法参数中的 spaceId。
     *
     * <p>优先查找标注 {@link SpaceId} 的参数,其次查找名为 "spaceId" 或 "id" 的参数。</p>
     */
    private Long resolveSpaceId(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 1. 查找 @SpaceId 注解
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(SpaceId.class)) {
                return toLong(args[i]);
            }
        }

        // 2. 查找名为 "spaceId" 或 "id" 的参数
        for (int i = 0; i < paramNames.length; i++) {
            if ("spaceId".equals(paramNames[i]) || "id".equals(paramNames[i])) {
                return toLong(args[i]);
            }
        }

        return null;
    }

    /**
     * 解析当前 actor 在目标空间中的角色。
     */
    private SpaceMemberRole resolveSpaceRole(AuthenticatedActor actor, Long spaceId) {
        if (actor.getKind() == AuthenticatedActor.Kind.API_KEY) {
            if (actor.getSpaceId() != null && actor.getSpaceId().equals(spaceId)) {
                return actor.getSpaceRole();
            }
            throw new UnauthorizedException("API Key not bound to space: " + spaceId);
        }
        // JWT 用户: 查 SpaceMember 表; ADMIN 绕过空间角色检查,视为 MANAGER
        if ("ADMIN".equals(actor.getUserRole())) {
            return SpaceMemberRole.MANAGER;
        }
        Optional<SpaceMember> member = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, actor.getUserId());
        return member.map(SpaceMember::getRole)
                .orElseThrow(() -> new UnauthorizedException("No access to space: " + spaceId));
    }

    private int rank(SpaceMemberRole role) {
        return switch (role) {
            case MANAGER -> 2;
            case MEMBER -> 1;
            case READER -> 0;
        };
    }

    private Long toLong(Object value) {
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
