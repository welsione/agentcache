package com.agentcache.server.security;

import com.agentcache.application.service.ApiKeyService;
import com.agentcache.application.service.ApiKeyService.ApiKeyValidation;
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
 * API Key 认证过滤器。
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                ApiKeyValidation validation = apiKeyService.validateApiKey(apiKey);
                AuthenticatedActor actor = new AuthenticatedActor();
                actor.setKind(AuthenticatedActor.Kind.API_KEY);
                actor.setApiKeyId(validation.apiKeyId());
                actor.setSpaceId(validation.spaceId());
                actor.setSpaceRole(validation.role());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        actor, null, List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                JwtAuthenticationFilter.markApiKeyAuthenticated(request);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}