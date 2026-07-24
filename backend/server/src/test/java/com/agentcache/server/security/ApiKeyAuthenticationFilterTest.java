package com.agentcache.server.security;

import com.agentcache.application.service.ApiKeyService;
import com.agentcache.application.service.ApiKeyService.ApiKeyValidation;
import com.agentcache.domain.enums.SpaceMemberRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * API Key 认证过滤器单元测试。
 */
class ApiKeyAuthenticationFilterTest {

    private ApiKeyService apiKeyService;
    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        apiKeyService = mock(ApiKeyService.class);
        filter = new ApiKeyAuthenticationFilter(apiKeyService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validApiKeyAuthenticates() throws Exception {
        when(apiKeyService.validateApiKey("ak-valid"))
                .thenReturn(new ApiKeyValidation(11L, 7L, SpaceMemberRole.MANAGER));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "ak-valid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        AuthenticatedActor principal = (AuthenticatedActor) authentication.getPrincipal();
        assertThat(principal.getApiKeyId()).isEqualTo(11L);
        assertThat(principal.getSpaceId()).isEqualTo(7L);
        assertThat(principal.getSpaceRole()).isEqualTo(SpaceMemberRole.MANAGER);
        assertThat(principal.getKind()).isEqualTo(AuthenticatedActor.Kind.API_KEY);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_API_KEY");
    }

    @Test
    void invalidApiKeyDoesNotAuthenticate() throws Exception {
        when(apiKeyService.validateApiKey(anyString()))
                .thenThrow(new com.agentcache.common.exception.UnauthorizedException("nope"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "ak-bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    void missingHeaderLeavesContextUnchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(apiKeyService, never()).validateApiKey(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}