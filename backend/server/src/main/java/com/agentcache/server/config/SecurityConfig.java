package com.agentcache.server.config;

import com.agentcache.common.response.Result;
import com.agentcache.server.security.ApiKeyAuthenticationFilter;
import com.agentcache.server.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper,
                                                              JwtProperties jwtProperties) {
        return (request, response, authException) -> {
            // 浏览器请求 (Accept: text/html) → 302 重定向到前端登录页
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html")) {
                String frontendBaseUrl = getFrontendBaseUrl(jwtProperties);
                // 将后端 API 路径转换为前端路由路径
                String frontendPath = toFrontendPath(request.getRequestURI(), request.getQueryString());
                String redirectUrl = frontendBaseUrl + "/login?redirect=" +
                        response.encodeRedirectURL(frontendPath);
                response.sendRedirect(redirectUrl);
                return;
            }
            // API 调用 (CLI/curl) → 401 JSON
            writeResult(response, objectMapper, HttpStatus.UNAUTHORIZED, 401, "Unauthenticated");
        };
    }

    /**
     * 获取前端基础 URL，默认从配置读取，未配置则使用本地开发地址。
     */
    private String getFrontendBaseUrl(JwtProperties jwtProperties) {
        String url = jwtProperties.getFrontendBaseUrl();
        if (url != null && !url.isBlank()) {
            return url;
        }
        return "http://localhost:5508";
    }

    /**
     * 将后端 API 路径转换为前端路由路径。
     * 例如 /api/files/3/content?spaceId=1 → /files/1/3
     * 无法识别的路径回退到 /dashboard。
     */
    private String toFrontendPath(String requestUri, String queryString) {
        if (requestUri != null && requestUri.matches(".*/api/files/(\\d+)/content.*")) {
            String fileId = requestUri.replaceAll(".*/api/files/(\\d+)/content.*", "$1");
            String spaceId = extractQueryParam(queryString, "spaceId");
            if (spaceId != null) {
                return "/files/" + spaceId + "/" + fileId;
            }
        }
        return "/dashboard";
    }

    /**
     * 从查询字符串中提取参数值。
     */
    private String extractQueryParam(String queryString, String name) {
        if (queryString == null || queryString.isBlank()) {
            return null;
        }
        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return kv[1];
            }
        }
        return null;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> writeResult(response, objectMapper,
                HttpStatus.FORBIDDEN, 403, "Forbidden");
    }

    private void writeResult(jakarta.servlet.http.HttpServletResponse response,
                             ObjectMapper objectMapper,
                             HttpStatus status,
                             int code,
                             String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.error(code, message));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                           AuthenticationEntryPoint entryPoint,
                                           AccessDeniedHandler deniedHandler) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/change-password").authenticated()
                .requestMatchers("/api/auth/invitations").authenticated()
                .requestMatchers("/api/auth/invitations/**").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(deniedHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}