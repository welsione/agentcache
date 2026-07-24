package com.agentcache.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件管理端到端集成测试。
 *
 * <p>覆盖登录、空间创建、文件上传/列表/下载、可见性切换、公开下载、
 * 删除、未授权访问等关键工作流。每个测试独立创建数据，方法顺序无关。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileFlowIntegrationTest {

    @BeforeAll
    static void configureHttpClient() {
        // 允许 JDK HttpURLConnection 缓冲错误流，避免 401 响应触发不可重试的 streaming 异常。
        System.setProperty("sun.net.http.errorstream.enableBuffering", "true");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    private TestRestTemplate restTemplate;
    private String baseUrl;
    private String jwt;

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = new TestRestTemplate(restTemplateBuilder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory()));
        baseUrl = "http://localhost:" + port;
        // 每个测试都重新登录获取独立的 JWT。
        jwt = loginAndGetJwt("admin", "admin@123");
        assertThat(jwt).isNotBlank();
    }

    @Test
    @Order(1)
    void loginReturnsJwtForValidCredentials() throws Exception {
        // 单独验证登录接口（覆盖错误路径以及 setUp 中成功路径）。
        // 错误密码：直接发 JSON 字符串，避免 HttpURLConnection 的 streaming 重试限制。
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> badReq = new HttpEntity<>(
                "{\"username\":\"admin\",\"password\":\"wrong\"}", jsonHeaders);
        ResponseEntity<String> bad = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, badReq, String.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpEntity<String> okReq = new HttpEntity<>(
                "{\"username\":\"admin\",\"password\":\"admin@123\"}", jsonHeaders);
        ResponseEntity<String> ok = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, okReq, String.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(ok.getBody());
        assertThat(root.path("code").asInt()).isEqualTo(200);
        assertThat(root.path("data").path("accessToken").asText()).isNotBlank();
        assertThat(root.path("data").path("tokenType").asText()).isEqualTo("Bearer");
    }

    @Test
    @Order(2)
    void createSpaceAndUploadListDownloadFlow() throws Exception {
        Long spaceId = createSpace("integ-space-" + System.nanoTime());
        assertThat(spaceId).isNotNull();

        byte[] payload = "Hello AgentCache Integration".getBytes(StandardCharsets.UTF_8);
        Long fileId = uploadFile(spaceId, "hello.txt", payload);
        assertThat(fileId).isNotNull();

        // 列表
        ResponseEntity<String> listResp = authedGet("/api/spaces/" + spaceId + "/files?q=hello");
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listBody = objectMapper.readTree(listResp.getBody());
        assertThat(listBody.path("data").path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(listBody.path("data").path("content").isArray()).isTrue();
        boolean found = false;
        for (JsonNode node : listBody.path("data").path("content")) {
            if (node.path("id").asLong() == fileId) {
                found = true;
                assertThat(node.path("originalName").asText()).isEqualTo("hello.txt");
                assertThat(node.path("accessUrl").asText()).contains("/api/files/" + fileId + "/content");
                assertThat(node.path("visibility").asText()).isEqualTo("PRIVATE");
                break;
            }
        }
        assertThat(found).as("uploaded file should appear in list").isTrue();

        // 下载
        ResponseEntity<byte[]> downloadResp = authedGetBytes("/api/files/" + fileId + "/content?spaceId=" + spaceId);
        assertThat(downloadResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResp.getBody()).isEqualTo(payload);
        assertThat(downloadResp.getHeaders().getContentType()).isNotNull();
        assertThat(downloadResp.getHeaders().getFirst("Content-Disposition"))
                .contains("attachment")
                .contains("hello.txt");
    }

    @Test
    @Order(3)
    void publicVisibilityEnablesAnonymousDownload() throws Exception {
        Long spaceId = createSpace("integ-public-" + System.nanoTime());
        byte[] payload = "Public Hello".getBytes(StandardCharsets.UTF_8);
        Long fileId = uploadFile(spaceId, "public.txt", payload);

        // 切换为 PUBLIC
        ResponseEntity<String> visResp = authedJson(HttpMethod.PUT,
                "/api/files/" + fileId + "/visibility?spaceId=" + spaceId,
                Map.of("visibility", "PUBLIC"));
        assertThat(visResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode visBody = objectMapper.readTree(visResp.getBody());
        assertThat(visBody.path("data").path("visibility").asText()).isEqualTo("PUBLIC");

        // 公开下载无需鉴权
        ResponseEntity<byte[]> publicResp = restTemplate.getForEntity(
                baseUrl + "/public/files/" + fileId + "/content", byte[].class);
        assertThat(publicResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicResp.getBody()).isEqualTo(payload);
    }

    @Test
    @Order(4)
    void deleteRemovesFileAndBlocksSubsequentDownload() throws Exception {
        Long spaceId = createSpace("integ-delete-" + System.nanoTime());
        byte[] payload = "to-be-deleted".getBytes(StandardCharsets.UTF_8);
        Long fileId = uploadFile(spaceId, "tmp.bin", payload);

        ResponseEntity<String> delResp = authedJson(HttpMethod.DELETE,
                "/api/files/" + fileId + "?spaceId=" + spaceId, null);
        assertThat(delResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 删除后再下载 → 404
        ResponseEntity<String> afterDelete = authedGet("/api/files/" + fileId + "/content?spaceId=" + spaceId);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(5)
    void unauthenticatedPrivateDownloadIsRejected() throws Exception {
        Long spaceId = createSpace("integ-auth-" + System.nanoTime());
        byte[] payload = "secret".getBytes(StandardCharsets.UTF_8);
        Long fileId = uploadFile(spaceId, "secret.txt", payload);

        // 不带任何凭证访问私有文件
        ResponseEntity<String> noAuth = restTemplate.getForEntity(
                baseUrl + "/api/files/" + fileId + "/content?spaceId=" + spaceId, String.class);
        assertThat(noAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- 辅助方法 -------------------------------------------------------------

    private String loginAndGetJwt(String username, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(
                objectMapper.writeValueAsString(Map.of("username", username, "password", password)), headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/auth/login", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(resp.getBody());
        return root.path("data").path("accessToken").asText();
    }

    private Long createSpace(String name) throws Exception {
        ResponseEntity<String> resp = authedJson(HttpMethod.POST, "/api/spaces",
                Map.of("name", name, "description", "integration test space"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(resp.getBody());
        return root.path("data").path("id").asLong();
    }

    private Long uploadFile(Long spaceId, String filename, byte[] payload) {
        LinkedMultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwt);

        HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(multipart, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl + "/api/spaces/" + spaceId + "/files", request, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            JsonNode root = objectMapper.readTree(resp.getBody());
            return root.path("data").path("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse upload response: " + resp.getBody(), e);
        }
    }

    private ResponseEntity<String> authedGet(String path) {
        return authedJson(HttpMethod.GET, path, null);
    }

    private ResponseEntity<byte[]> authedGetBytes(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(baseUrl + path, HttpMethod.GET, entity, byte[].class);
    }

    /**
     * 携带 JWT 发送 JSON 请求。{@code body} 为 {@code null} 时不发送 body。
     */
    private ResponseEntity<String> authedJson(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
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