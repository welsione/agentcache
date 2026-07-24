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
 * 用户管理与权限集成测试。
 *
 * <p>覆盖 /api/users/me、用户列表(仅 ADMIN)、角色/状态变更、ADMIN 重置密码、
 * 空间成员管理及 @RequireAdmin/@RequireSpaceRole 权限拦截等关键工作流。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserManagementIntegrationTest {

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
    void getCurrentUserReturnsAdminInfo() throws Exception {
        ResponseEntity<String> resp = authedGet(adminJwt, "/api/users/me");
        JsonNode body = objectMapper.readTree(resp.getBody());
        assertThat(body.path("data").path("username").asText()).isEqualTo("admin");
        assertThat(body.path("data").path("role").asText()).isEqualTo("ADMIN");
    }

    @Test
    void userListRequiresAdmin() throws Exception {
        // 通过邀请创建普通用户
        String userJwt = createInvitee("plain-user-" + System.nanoTime(), "plain-pw");

        // 普通用户访问用户列表 -> 403
        ResponseEntity<String> forbidden = authedGet(userJwt, "/api/users");
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // ADMIN 访问用户列表 -> 200
        ResponseEntity<String> ok = authedGet(adminJwt, "/api/users?page=0&size=20");
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(ok.getBody());
        assertThat(body.path("data").path("content").isArray()).isTrue();
    }

    @Test
    void adminChangesUserRoleAndStatus() throws Exception {
        String username = "role-target-" + System.nanoTime();
        Long userId = createInviteeUser(username, "target-pw");

        // 切换为 ADMIN
        ResponseEntity<String> roleResp = authedJson(adminJwt, HttpMethod.PUT,
                "/api/users/" + userId + "/role", Map.of("role", "ADMIN"));
        assertThat(roleResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode roleBody = objectMapper.readTree(roleResp.getBody());
        assertThat(roleBody.path("data").path("role").asText()).isEqualTo("ADMIN");

        // 禁用用户
        ResponseEntity<String> statusResp = authedJson(adminJwt, HttpMethod.PUT,
                "/api/users/" + userId + "/status", Map.of("status", "DELETED"));
        assertThat(statusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode statusBody = objectMapper.readTree(statusResp.getBody());
        assertThat(statusBody.path("data").path("status").asText()).isEqualTo("DELETED");

        // 禁用后该用户登录应失败
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> loginReq = new HttpEntity<>(objectMapper.writeValueAsString(
                Map.of("username", username, "password", "target-pw")), headers);
        ResponseEntity<String> loginResp = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, loginReq, String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminResetPasswordForcesChangeOnNextLogin() throws Exception {
        String username = "reset-target-" + System.nanoTime();
        Long userId = createInviteeUser(username, "initial-pw");

        // ADMIN 重置密码
        ResponseEntity<String> resp = authedJson(adminJwt, HttpMethod.PUT,
                "/api/users/" + userId + "/password", Map.of("newPassword", "reset-pw-123"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 用户用新密码登录,mustChangePassword 应为 true
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> loginReq = new HttpEntity<>(objectMapper.writeValueAsString(
                Map.of("username", username, "password", "reset-pw-123")), headers);
        ResponseEntity<String> loginResp = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, loginReq, String.class);
        JsonNode body = objectMapper.readTree(loginResp.getBody());
        assertThat(body.path("data").path("mustChangePassword").asBoolean()).isTrue();
    }

    @Test
    void spaceMemberManagementRequiresManager() throws Exception {
        Long spaceId = createSpace("member-space-" + System.nanoTime());

        // 列出成员
        ResponseEntity<String> listResp = authedGet(adminJwt, "/api/spaces/" + spaceId + "/members");
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listBody = objectMapper.readTree(listResp.getBody());
        assertThat(listBody.path("data").isArray()).isTrue();
        // 创建者自动成为 MANAGER 成员
        assertThat(listBody.path("data").size()).isGreaterThanOrEqualTo(1);

        // 添加一个成员
        Long memberId = createInviteeUser("new-member-" + System.nanoTime(), "member-pw");
        ResponseEntity<String> addResp = authedJson(adminJwt, HttpMethod.POST,
                "/api/spaces/" + spaceId + "/members", Map.of("userId", memberId, "role", "MEMBER"));
        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(addResp.getBody()).path("data").path("role").asText())
                .isEqualTo("MEMBER");

        // 修改成员角色为 READER
        ResponseEntity<String> roleResp = authedJson(adminJwt, HttpMethod.PUT,
                "/api/spaces/" + spaceId + "/members/" + memberId, Map.of("role", "READER"));
        assertThat(roleResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(roleResp.getBody()).path("data").path("role").asText())
                .isEqualTo("READER");

        // 移除成员
        ResponseEntity<String> delResp = authedJson(adminJwt, HttpMethod.DELETE,
                "/api/spaces/" + spaceId + "/members/" + memberId, null);
        assertThat(delResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void nonMemberCannotAccessSpace() throws Exception {
        Long spaceId = createSpace("private-space-" + System.nanoTime());
        String outsiderJwt = createInvitee("outsider-" + System.nanoTime(), "out-pw");

        ResponseEntity<String> resp = authedGet(outsiderJwt, "/api/spaces/" + spaceId);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void duplicateMemberRejected() throws Exception {
        Long spaceId = createSpace("dup-space-" + System.nanoTime());
        Long adminId = objectMapper.readTree(authedGet(adminJwt, "/api/users/me").getBody())
                .path("data").path("id").asLong();

        ResponseEntity<String> resp = authedJson(adminJwt, HttpMethod.POST,
                "/api/spaces/" + spaceId + "/members", Map.of("userId", adminId, "role", "MEMBER"));
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void unauthenticatedAccessRejected() {
        ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl + "/api/users/me", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- 辅助方法 -------------------------------------------------------------

    private String createInvitee(String username, String password) throws Exception {
        createInviteeUser(username, password);
        return loginAndGetJwt(username, password);
    }

    private Long createInviteeUser(String username, String password) throws Exception {
        // 创建邀请令牌
        HttpHeaders headers = authedHeaders(adminJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> invReq = new HttpEntity<>("{}", headers);
        ResponseEntity<String> invResp = restTemplate.exchange(
                baseUrl + "/api/auth/invitations", HttpMethod.POST, invReq, String.class);
        String token = objectMapper.readTree(invResp.getBody())
                .path("data").path("token").asText();

        // 接受邀请(mustChangePassword=true,但登录仍可拿到 token)
        HttpEntity<String> acceptReq = new HttpEntity<>(objectMapper.writeValueAsString(Map.of(
                "token", token,
                "username", username,
                "email", username + "@example.com",
                "password", password)), headers);
        ResponseEntity<String> acceptResp = restTemplate.exchange(
                baseUrl + "/api/auth/invite-accept", HttpMethod.POST, acceptReq, String.class);
        assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 登录拿 token,再取 /api/users/me 得到 userId
        String jwt = loginAndGetJwt(username, password);
        ResponseEntity<String> meResp = authedGet(jwt, "/api/users/me");
        return objectMapper.readTree(meResp.getBody()).path("data").path("id").asLong();
    }

    private Long createSpace(String name) throws Exception {
        ResponseEntity<String> resp = authedJson(adminJwt, HttpMethod.POST, "/api/spaces",
                Map.of("name", name, "description", "test space"));
        return objectMapper.readTree(resp.getBody()).path("data").path("id").asLong();
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

    private ResponseEntity<String> authedGet(String token, String path) {
        return authedJson(token, HttpMethod.GET, path, null);
    }

    private HttpHeaders authedHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<String> authedJson(String token, HttpMethod method, String path, Object body) {
        HttpHeaders headers = authedHeaders(token);
        HttpEntity<String> entity;
        if (body == null) {
            entity = new HttpEntity<>(headers);
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
            try {
                entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize request body", e);
            }
        }
        return restTemplate.exchange(baseUrl + path, method, entity, String.class);
    }
}
