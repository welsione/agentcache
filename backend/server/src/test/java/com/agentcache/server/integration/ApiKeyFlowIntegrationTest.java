package com.agentcache.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API Key 端到端集成测试。
 *
 * <p>覆盖 API Key 创建、API Key 认证访问空间与文件、上传文件时 createdBy 为空、
 * READER 角色写操作被拒绝等关键工作流。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiKeyFlowIntegrationTest {

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
    @Order(1)
    void apiKeyCanAccessOwnSpaceAndFiles() throws Exception {
        Long spaceId = createSpace("integ-apikey-" + System.nanoTime());
        String plainKey = createApiKey(spaceId, "agent1", "MEMBER");
        assertThat(plainKey).startsWith("ak-");

        // API Key 访问空间列表，应只看到绑定空间。
        ResponseEntity<String> spacesResp = apiKeyGet(plainKey, "/api/spaces");
        assertThat(spacesResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode spacesBody = objectMapper.readTree(spacesResp.getBody());
        boolean found = false;
        for (JsonNode node : spacesBody.path("data")) {
            if (node.path("id").asLong() == spaceId) {
                found = true;
                break;
            }
        }
        assertThat(found).as("API Key should see its bound space").isTrue();

        // API Key 访问文件列表。
        ResponseEntity<String> filesResp = apiKeyGet(plainKey, "/api/spaces/" + spaceId + "/files");
        assertThat(filesResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode filesBody = objectMapper.readTree(filesResp.getBody());
        assertThat(filesBody.path("data").path("content").isArray()).isTrue();
    }

    @Test
    @Order(2)
    void apiKeyUploadSetsCreatedByToNull() throws Exception {
        Long spaceId = createSpace("integ-apikey-upload-" + System.nanoTime());
        String plainKey = createApiKey(spaceId, "agent-uploader", "MEMBER");

        byte[] payload = "uploaded by agent".getBytes(StandardCharsets.UTF_8);
        Long fileId = apiKeyUploadFile(plainKey, spaceId, "agent.bin", payload);

        ResponseEntity<String> metaResp = apiKeyGet(plainKey, "/api/files/" + fileId + "?spaceId=" + spaceId);
        assertThat(metaResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode metaBody = objectMapper.readTree(metaResp.getBody());
        JsonNode file = metaBody.path("data");
        assertThat(file.path("id").asLong()).isEqualTo(fileId);
        assertThat(file.path("createdBy").isNull()).as("API Key 上传文件时 createdBy 应为 null").isTrue();
        assertThat(file.path("size").asLong()).isEqualTo(payload.length);
    }

    @Test
    @Order(3)
    void readerApiKeyCannotUpload() throws Exception {
        Long spaceId = createSpace("integ-apikey-reader-" + System.nanoTime());
        String plainKey = createApiKey(spaceId, "agent-reader", "READER");

        LinkedMultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new ByteArrayResource("x".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "forbidden.txt";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-Key", plainKey);
        HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(multipart, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl + "/api/spaces/" + spaceId + "/files", request, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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

    private Long createSpace(String name) {
        return createSpace(name, adminJwt);
    }

    private Long createSpace(String name, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<String> req;
        try {
            req = new HttpEntity<>(objectMapper.writeValueAsString(
                    Map.of("name", name, "description", "integration test space")), headers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/spaces", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody()).path("data").path("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse space response: " + resp.getBody(), e);
        }
    }

    private String createApiKey(Long spaceId, String name, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminJwt);
        HttpEntity<String> req;
        try {
            req = new HttpEntity<>(objectMapper.writeValueAsString(
                    Map.of("name", name, "role", role)), headers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/api/spaces/" + spaceId + "/api-keys", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody()).path("data").path("apiKey").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse api key response: " + resp.getBody(), e);
        }
    }

    private Long apiKeyUploadFile(String apiKey, Long spaceId, String filename, byte[] payload) {
        LinkedMultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-Key", apiKey);
        HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(multipart, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl + "/api/spaces/" + spaceId + "/files", request, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(resp.getBody()).path("data").path("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse upload response: " + resp.getBody(), e);
        }
    }

    private ResponseEntity<String> apiKeyGet(String apiKey, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(baseUrl + path, HttpMethod.GET, entity, String.class);
    }
}