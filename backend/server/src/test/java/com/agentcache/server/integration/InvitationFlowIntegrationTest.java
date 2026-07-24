package com.agentcache.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 邀请注册与强制改密端到端集成测试。
 *
 * <p>覆盖 ADMIN 创建邀请、凭邀请注册、新用户强制改密、登录响应携带 mustChangePassword、
 * 改密后状态清除等关键工作流。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InvitationFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    private TestRestTemplate restTemplate;
    private String baseUrl;
    private String adminJwt;

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = new TestRestTemplate(restTemplateBuilder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory()));
        baseUrl = "http://localhost:" + port;
        adminJwt = loginAndGetJwt("admin", "admin@123");
        assertThat(adminJwt).isNotBlank();
    }

    @Test
    void adminLoginReportsMustChangePassword() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(
                objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin@123")), headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, req, String.class);
        JsonNode root = objectMapper.readTree(resp.getBody());
        assertThat(root.path("data").path("mustChangePassword").asBoolean()).isTrue();
        assertThat(root.path("data").path("role").asText()).isEqualTo("ADMIN");
        assertThat(root.path("data").path("userId").asLong()).isPositive();
    }

    @Test
    void inviteAcceptCreatesUserWhoMustChangePassword() throws Exception {
        String inviteUrl = createInvitation();
        String token = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);

        // 凭令牌注册
        String username = "invitee-" + System.nanoTime();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                "token", token,
                "username", username,
                "email", username + "@example.com",
                "password", "invitee-pw")), headers);
        ResponseEntity<String> acceptResp = restTemplate.exchange(
                baseUrl + "/api/auth/invite-accept", HttpMethod.POST, req, String.class);
        assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 新用户登录,mustChangePassword 应为 true
        HttpEntity<String> loginReq = new HttpEntity<>(objectMapper.writeValueAsString(
                Map.of("username", username, "password", "invitee-pw")), headers);
        ResponseEntity<String> loginResp = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, loginReq, String.class);
        JsonNode loginBody = objectMapper.readTree(loginResp.getBody());
        assertThat(loginBody.path("data").path("mustChangePassword").asBoolean()).isTrue();
        assertThat(loginBody.path("data").path("role").asText()).isEqualTo("USER");

        // 令牌已被标记为使用,重复注册应失败
        ResponseEntity<String> reuse = restTemplate.exchange(
                baseUrl + "/api/auth/invite-accept", HttpMethod.POST, req, String.class);
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePasswordClearsMustChangePassword() throws Exception {
        String inviteUrl = createInvitation();
        String token = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);
        String username = "pw-changer-" + System.nanoTime();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> acceptReq = new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                "token", token,
                "username", username,
                "email", username + "@example.com",
                "password", "initial-pw")), headers);
        restTemplate.exchange(baseUrl + "/api/auth/invite-accept", HttpMethod.POST, acceptReq, String.class);

        String jwt = loginAndGetJwt(username, "initial-pw");
        assertThat(jwt).isNotBlank();

        // 修改密码(需带 Content-Type: application/json)
        HttpHeaders changeHeaders = authedHeaders(jwt);
        changeHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> changeReq = new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                "oldPassword", "initial-pw",
                "newPassword", "brand-new-pw")), changeHeaders);
        ResponseEntity<String> changeResp = restTemplate.exchange(
                baseUrl + "/api/auth/change-password", HttpMethod.POST, changeReq, String.class);
        assertThat(changeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 重新登录,mustChangePassword 应为 false
        HttpEntity<String> loginReq = new HttpEntity<>(objectMapper.writeValueAsString(
                Map.of("username", username, "password", "brand-new-pw")), headers);
        ResponseEntity<String> loginResp = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, loginReq, String.class);
        JsonNode loginBody = objectMapper.readTree(loginResp.getBody());
        assertThat(loginBody.path("data").path("mustChangePassword").asBoolean()).isFalse();
    }

    @Test
    void changePasswordRejectsWrongOldPassword() throws Exception {
        HttpHeaders headers = authedHeaders(adminJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(
                Map.of("oldPassword", "wrong-old", "newPassword", "new-pw-123")), headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/auth/change-password", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredTokenCannotRegister() throws Exception {
        // 创建邀请并解析其 id 与 token
        HttpHeaders headers = authedHeaders(adminJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>("{}", headers);
        ResponseEntity<String> createResp = restTemplate.exchange(
                baseUrl + "/api/auth/invitations", HttpMethod.POST, req, String.class);
        JsonNode created = objectMapper.readTree(createResp.getBody()).path("data");
        Long invitationId = created.path("id").asLong();
        String token = created.path("token").asText();

        // 撤销令牌使其过期
        HttpEntity<Void> revokeReq = new HttpEntity<>(authedHeaders(adminJwt));
        ResponseEntity<String> revokeResp = restTemplate.exchange(
                baseUrl + "/api/auth/invitations/" + invitationId, HttpMethod.DELETE, revokeReq, String.class);
        assertThat(revokeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 撤销后注册应失败
        HttpEntity<String> acceptReq = new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                "token", token,
                "username", "expired-user",
                "email", "expired@example.com",
                "password", "some-pw-123")), headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/auth/invite-accept", HttpMethod.POST, acceptReq, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePasswordRequiresAuthentication() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>("{\"oldPassword\":\"x\",\"newPassword\":\"y\"}", headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/auth/change-password", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- 辅助方法 -------------------------------------------------------------

    private String createInvitation() throws Exception {
        HttpHeaders headers = authedHeaders(adminJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>("{}", headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/auth/invitations", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resp.getBody()).path("data").path("inviteUrl").asText();
    }

    private String loginAndGetJwt(String username, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(
                objectMapper.writeValueAsString(Map.of("username", username, "password", password)), headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, req, String.class);
        return objectMapper.readTree(resp.getBody()).path("data").path("accessToken").asText();
    }

    private HttpHeaders authedHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
