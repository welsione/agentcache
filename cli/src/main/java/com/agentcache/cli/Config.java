package com.agentcache.cli;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CLI 持久化配置（{@code ~/.agentcache/config.yaml}）。
 *
 * <p>字段:</p>
 * <ul>
 *   <li>{@code serverUrl} - 后端服务地址，默认 {@code http://localhost:8080}</li>
 *   <li>{@code apiKey} - API Key（明文仅落盘到本地文件）</li>
 *   <li>{@code defaultSpace} - 默认空间 ID</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class Config {

    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";

    private String serverUrl = DEFAULT_SERVER_URL;
    private String apiKey;
    private Long defaultSpace;

    /**
     * 返回配置文件路径：{@code ~/.agentcache/config.yaml}。
     */
    public static Path getConfigPath() {
        return Paths.get(System.getProperty("user.home"), ".agentcache", "config.yaml");
    }

    /**
     * 从磁盘加载配置；不存在则返回默认值。
     *
     * @throws IOException 读取失败
     */
    @SuppressWarnings("unchecked")
    public static Config load() throws IOException {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return new Config();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Map<String, Object> root = new Yaml().load(reader);
            if (root == null) {
                return new Config();
            }
            return fromMap(root);
        }
    }

    /**
     * 写到 {@link #getConfigPath()}，必要时创建父目录。
     *
     * @throws IOException 写入失败
     */
    public void save() throws IOException {
        Path path = getConfigPath();
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Map<String, Object> data = toMap();
        try (Writer writer = Files.newBufferedWriter(path)) {
            new Yaml().dump(data, writer);
        }
    }

    /**
     * 校验当前配置：serverUrl 必须以 {@code http://} 或 {@code https://} 开头。
     *
     * @throws IllegalArgumentException 校验失败
     */
    public void validate() {
        if (serverUrl == null
                || !(serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))) {
            throw new IllegalArgumentException(
                    "serverUrl must start with http:// or https://, got: " + serverUrl);
        }
    }

    @SuppressWarnings("unchecked")
    private static Config fromMap(Map<String, Object> root) {
        Config config = new Config();
        Map<String, Object> server = asMap(root.get("server"));
        if (server != null) {
            Object url = server.get("url");
            if (url != null) {
                config.serverUrl = url.toString();
            }
        }
        Map<String, Object> auth = asMap(root.get("auth"));
        if (auth != null) {
            Object key = auth.get("apiKey");
            if (key != null) {
                config.apiKey = key.toString();
            }
            Object space = auth.get("defaultSpace");
            if (space != null) {
                config.defaultSpace = parseLong(space);
            }
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Map<String, Object> toMap() {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", serverUrl);
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("apiKey", apiKey);
        if (defaultSpace != null) {
            auth.put("defaultSpace", defaultSpace);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("server", server);
        root.put("auth", auth);
        return root;
    }
}