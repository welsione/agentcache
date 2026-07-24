package com.agentcache.server.security;

import com.agentcache.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_AUTH_ATTR = "agentcache.apiKeyAuthenticated";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = tokenProvider.parseToken(token);
                AuthenticatedActor actor = new AuthenticatedActor();
                actor.setKind(AuthenticatedActor.Kind.USER);
                actor.setUserId(Long.valueOf(claims.getSubject()));
                actor.setUsername(claims.get("username", String.class));
                actor.setUserRole(claims.get("role", String.class));
                String authority = resolveAuthority(actor.getUserRole());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        actor, null, List.of(new SimpleGrantedAuthority(authority)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // 仅当 API Key 过滤器未成功认证时才清空上下文，避免覆盖 API Key 认证结果。
                Boolean apiKeyOk = (Boolean) request.getAttribute(API_KEY_AUTH_ATTR);
                if (!Boolean.TRUE.equals(apiKeyOk)) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    static void markApiKeyAuthenticated(jakarta.servlet.http.HttpServletRequest request) {
        request.setAttribute(API_KEY_AUTH_ATTR, Boolean.TRUE);
    }

    private String resolveAuthority(String role) {
        if (role != null && UserRole.ADMIN.name().equals(role)) {
            return "ROLE_ADMIN";
        }
        return "ROLE_USER";
    }
}