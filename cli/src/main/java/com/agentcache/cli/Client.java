package com.agentcache.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 封装 HTTP 调用，复用 {@link CloseableHttpClient} 单例。
 *
 * <p>所有方法在响应 {@code code != 200} 或 HTTP 非 2xx 时抛 {@link CliException}。
 * API Key 通过 {@code X-API-Key} header 注入，绝不写入日志。</p>
 */
@Slf4j
public class Client implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Config config;
    @Getter
    private final CloseableHttpClient http;

    public Client(Config config) {
        this.config = config;
        this.http = buildHttpClient();
    }

    private static CloseableHttpClient buildHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(10))
                        .setSocketTimeout(Timeout.ofSeconds(30))
                        .build())
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(60))
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManagerShared(true)
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }

    /**
     * GET 请求并反序列化 {@code data} 到指定类型。
     */
    public <T> T get(String path, Class<T> responseType, Map<String, String> query) throws IOException {
        return executeWithQuery(new HttpGet(resolve(path)), query, responseType);
    }

    /**
     * GET 请求并反序列化 {@code data} 到 {@link TypeReference} 类型（用于泛型响应）。
     */
    public <T> T get(String path, TypeReference<T> typeRef, Map<String, String> query) throws IOException {
        return executeWithQuery(new HttpGet(resolve(path)), query, typeRef);
    }

    /**
     * POST JSON 请求。
     */
    public <T> T postJson(String path, Object body, Class<T> responseType) throws IOException {
        HttpPost request = new HttpPost(resolve(path));
        applyAuth(request);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));
        return execute(request, responseType);
    }

    /**
     * PUT JSON 请求，可附带 query 参数。
     */
    public <T> T putJson(String path, Object body, Class<T> responseType, Map<String, String> query) throws IOException {
        return executeWithQuery(buildJsonPut(path, body), query, responseType);
    }

    private HttpPut buildJsonPut(String path, Object body) throws IOException {
        HttpPut request = new HttpPut(resolve(path));
        applyAuth(request);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));
        return request;
    }

    /**
     * POST multipart/form-data 上传单个文件。
     */
    public <T> T postForm(String path, Path file, Class<T> responseType) throws IOException {
        HttpPost request = new HttpPost(resolve(path));
        applyAuth(request);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.LEGACY)
                .addBinaryBody("file", file.toFile(), ContentType.DEFAULT_BINARY, file.getFileName().toString());
        request.setEntity(builder.build());
        return execute(request, responseType);
    }

    /**
     * DELETE 请求。
     */
    public void delete(String path, Map<String, String> query) throws IOException {
        HttpDelete request = new HttpDelete(resolve(path));
        applyAuth(request);
        executeRaw(request, (status, body) -> {
            if (status / 100 != 2) {
                throw new CliException("HTTP " + status + ": " + body, status, status);
            }
            return null;
        });
    }

    /**
     * 流式下载文件到 {@code dest}。返回 dest。
     */
    public Path download(String path, Path dest) throws IOException {
        if (dest.getParent() != null) {
            Files.createDirectories(dest.getParent());
        }
        HttpGet request = new HttpGet(resolve(path));
        applyAuth(request);
        return http.execute(request, response -> {
            int status = response.getCode();
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new CliException("HTTP " + status + ": empty body", status, status);
            }
            try (InputStream in = entity.getContent()) {
                if (status / 100 != 2) {
                    String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    throw new CliException("HTTP " + status + ": " + body, status, status);
                }
                Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return dest;
        });
    }

    @Override
    public void close() throws IOException {
        http.close();
    }

    // ---- 内部工具 ----

    private URI resolve(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path);
        }
        String base = config.getServerUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String suffix = path.startsWith("/") ? path : "/" + path;
        return URI.create(base + suffix);
    }

    private void applyAuth(HttpUriRequestBase request) {
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            request.setHeader("X-API-Key", config.getApiKey());
        }
    }

    private <T> T executeWithQuery(HttpUriRequestBase request, Map<String, String> query, Class<T> responseType)
            throws IOException {
        applyQuery(request, query);
        applyAuth(request);
        return executeRaw(request, (status, body) -> parseResponse(status, body, responseType));
    }

    private <T> T executeWithQuery(HttpUriRequestBase request, Map<String, String> query, TypeReference<T> typeRef)
            throws IOException {
        applyQuery(request, query);
        applyAuth(request);
        return executeRaw(request, (status, body) -> parseResponse(status, body, typeRef));
    }

    private void applyQuery(HttpUriRequestBase request, Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            sb.append(sb.length() == 0 ? '?' : '&')
                    .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        try {
            request.setUri(URI.create(request.getUri().toString() + sb));
        } catch (java.net.URISyntaxException ex) {
            throw new IllegalStateException("Invalid URI on request: " + ex.getMessage(), ex);
        }
    }

    private <T> T execute(HttpUriRequestBase request, Class<T> responseType) throws IOException {
        applyAuth(request);
        return executeRaw(request, (status, body) -> parseResponse(status, body, responseType));
    }

    private <T> T executeRaw(HttpUriRequestBase request, ResponseHandler<T> handler) throws IOException {
        return http.execute(request, response -> {
            int status = response.getCode();
            String body = readBody(response.getEntity());
            log.debug("HTTP {} {} -> {} bytes",
                    request.getMethod(), request.getRequestUri(), body.length());
            return handler.handle(status, body);
        });
    }

    private <T> T parseResponse(int status, String body, Class<T> responseType) {
        if (status / 100 != 2) {
            throw new CliException("HTTP " + status + ": " + body, status, status);
        }
        try {
            return readData(body, responseType);
        } catch (CliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CliException("Failed to parse response: " + ex.getMessage(),
                    0, status, ex);
        }
    }

    private <T> T parseResponse(int status, String body, TypeReference<T> typeRef) {
        if (status / 100 != 2) {
            throw new CliException("HTTP " + status + ": " + body, status, status);
        }
        try {
            return readData(body, typeRef);
        } catch (CliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CliException("Failed to parse response: " + ex.getMessage(),
                    0, status, ex);
        }
    }

    private <T> T readData(String body, Class<T> responseType) throws IOException {
        ApiResult raw = MAPPER.readValue(body, ApiResult.class);
        if (raw.getCode() != 200) {
            throw new CliException(raw.getMessage() == null ? "request failed" : raw.getMessage(),
                    raw.getCode());
        }
        if (raw.getData() == null) {
            return null;
        }
        String dataJson = MAPPER.writeValueAsString(raw.getData());
        return MAPPER.readValue(dataJson, responseType);
    }

    private <T> T readData(String body, TypeReference<T> typeRef) throws IOException {
        ApiResult raw = MAPPER.readValue(body, new TypeReference<ApiResult<Object>>() {});
        if (raw.getCode() != 200) {
            throw new CliException(raw.getMessage() == null ? "request failed" : raw.getMessage(),
                    raw.getCode());
        }
        if (raw.getData() == null) {
            return null;
        }
        String dataJson = MAPPER.writeValueAsString(raw.getData());
        return MAPPER.readValue(dataJson, typeRef);
    }

    private static String readBody(HttpEntity entity) throws IOException {
        if (entity == null) {
            return "";
        }
        try (InputStream in = entity.getContent()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @FunctionalInterface
    private interface ResponseHandler<T> {
        T handle(int status, String body) throws IOException;
    }

    /**
     * 后端 {@code Result} 的镜像结构，用于反序列化。
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApiResult<T> {
        private int code;
        private String message;
        private T data;
    }
}