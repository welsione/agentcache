package com.agentcache.server.security;

import com.agentcache.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 从 Spring Security 上下文解析出当前请求的 {@link AuthenticatedActor}。
 *
 * <p>过滤器链将 {@link AuthenticatedActor} 写入 {@code Authentication.getPrincipal()}，
 * 控制器通过该工具类取出，避免重复处理 SecurityContext。</p>
 */
@Component
public class RequestActorResolver {

    /**
     * 获取当前请求的认证主体；未认证时抛 {@link UnauthorizedException}。
     *
     * @return 当前认证主体
     */
    public AuthenticatedActor require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedActor actor)) {
            throw new UnauthorizedException("Unauthenticated");
        }
        return actor;
    }

    /**
     * 获取当前请求的认证主体；未认证时返回 {@code null}。
     *
     * @return 当前认证主体，可能为 {@code null}
     */
    public AuthenticatedActor currentOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AuthenticatedActor actor) {
            return actor;
        }
        return null;
    }
}