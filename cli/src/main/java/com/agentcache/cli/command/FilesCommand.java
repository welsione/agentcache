package com.agentcache.cli.command;

import com.agentcache.cli.Client;
import com.agentcache.cli.Config;
import com.agentcache.cli.dto.FileResponse;
import com.agentcache.cli.dto.PageResponse;
import com.agentcache.cli.dto.VisibilityUpdateRequest;
import com.agentcache.cli.output.Printer;
import com.fasterxml.jackson.core.type.TypeReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * {@code files} 子命令组：列出、上传、下载、修改可见性等。
 */
@Command(name = "files", mixinStandardHelpOptions = true,
        description = "Manage files in a space",
        subcommands = {
                FilesCommand.ListCommand.class,
                FilesCommand.UploadCommand.class,
                FilesCommand.DownloadCommand.class,
                FilesCommand.InfoCommand.class,
                FilesCommand.LinkCommand.class,
                FilesCommand.ShareCommand.class,
                FilesCommand.PrivateCommand.class,
                FilesCommand.DeleteCommand.class
        })
public class FilesCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Usage: agentcache files <list|upload|download|info|link|share|private|delete>");
        return 0;
    }

    /**
     * 共享参数：{@code --space} 可缺省，取 {@code config.defaultSpace}。
     */
    abstract static class FileSubcommand implements Callable<Integer> {

        @Option(names = {"--space"}, description = "Space ID (defaults to config.defaultSpace)")
        Long spaceId;

        protected Long resolveSpaceId(Config config) {
            return spaceId != null ? spaceId : config.getDefaultSpace();
        }

        protected Long requireSpaceId(Config config, Printer printer) {
            Long resolved = resolveSpaceId(config);
            if (resolved == null) {
                printer.printError("No space specified and no default space configured. "
                        + "Use --space <id> or run 'agentcache login <apiKey> --space <id>'.");
            }
            return resolved;
        }

        /**
         * 设置可见性并输出访问链接。
         */
        protected Integer runVisibilityChange(Long fileId, String visibility) throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = SpacesCommand.requireAuth(printer);
            if (config == null) {
                return 2;
            }
            Long sid = requireSpaceId(config, printer);
            if (sid == null) {
                return 2;
            }
            Map<String, String> query = new LinkedHashMap<>();
            query.put("spaceId", String.valueOf(sid));
            VisibilityUpdateRequest body = new VisibilityUpdateRequest();
            body.setVisibility(visibility);
            try (Client client = new Client(config)) {
                FileResponse resp = client.putJson(
                        "/api/files/" + fileId + "/visibility", body, FileResponse.class, query);
                String base = config.getServerUrl();
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
                String url = base + (resp.getAccessUrl() == null ? "" : resp.getAccessUrl());
                if (Printer.isJson()) {
                    printer.printObject(new ShareResult(url, resp.getVisibility()));
                } else {
                    System.out.println("Visibility: " + resp.getVisibility());
                    System.out.println("Access URL: " + url);
                }
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }
    }

    /**
     * 列出文件。
     */
    @Command(name = "list", mixinStandardHelpOptions = true, description = "List files in a space")
    public static class ListCommand extends FileSubcommand {

        @Option(names = {"--query", "-q"}, description = "Search query")
        String query;

        @Parameters(arity = "0..1", description = "Page number (zero-based)")
        Integer page;

        @Option(names = {"--size"}, description = "Page size", defaultValue = "20")
        int size;

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = SpacesCommand.requireAuth(printer);
            if (config == null) {
                return 2;
            }
            Long sid = requireSpaceId(config, printer);
            if (sid == null) {
                return 2;
            }
            int pageNum = page == null ? 0 : page;
            Map<String, String> queryMap = new LinkedHashMap<>();
            queryMap.put("page", String.valueOf(pageNum));
            queryMap.put("size", String.valueOf(size));
            if (query != null && !query.isEmpty()) {
                queryMap.put("q", query);
            }
            try (Client client = new Client(config)) {
                PageResponse<FileResponse> pageResp = client.get(
                        "/api/spaces/" + sid + "/files",
                        new TypeReference<PageResponse<FileResponse>>() {},
                        queryMap);
                if (Printer.isJson()) {
                    printer.printObject(pageResp);
                } else {
                    List<FileResponse> content = pageResp.getContent() == null ? List.of() : pageResp.getContent();
                    if (content.isEmpty()) {
                        System.out.println("(no files)");
                    } else {
                        List<String[]> rows = new ArrayList<>();
                        rows.add(new String[]{"ID", "Name", "Size", "Visibility", "Created"});
                        for (FileResponse f : content) {
                            rows.add(new String[]{
                                    String.valueOf(f.getId()),
                                    nullSafe(f.getOriginalName()),
                                    humanSize(f.getSize()),
                                    nullSafe(f.getVisibility()),
                                    f.getCreatedAt() == null ? "" : f.getCreatedAt().toString()
                            });
                        }
                        printer.printTable(rows);
                    }
                    System.out.println("Total: " + pageResp.getTotal());
                }
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }
    }

    /**
     * 上传文件。
     */
    @Command(name = "upload", mixinStandardHelpOptions = true, description = "Upload a file to a space")
    public static class UploadCommand extends FileSubcommand {

        @Parameters(index = "0", description = "Local file path to upload")
        Path localPath;

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = SpacesCommand.requireAuth(printer);
            if (config == null) {
                return 2;
            }
            Long sid = requireSpaceId(config, printer);
            if (sid == null) {
                return 2;
            }
            if (!Files.exists(localPath) || !Files.isRegularFile(localPath)) {
                printer.printError("Local file not found: " + localPath);
                return 2;
            }
            try (Client client = new Client(config)) {
                FileResponse resp = client.postForm(
                        "/api/spaces/" + sid + "/files", localPath, FileResponse.class);
                printer.printObject(resp);
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }
    }

    /**
     * 下载文件。
     */
    @Command(name = "download", mixinStandardHelpOptions = true, description = "Download a file")
    public static class DownloadCommand extends FileSubcommand {

        @Parameters(index = "0", description = "File ID")
        Long fileId;

        @Option(names = {"--output", "-o"}, required = true,
                description = "Output directory (filename derived from server)")
        Path output;

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = SpacesCommand.requireAuth(printer);
            if (config == null) {
                return 2;
            }
            Long sid = requireSpaceId(config, printer);
            if (sid == null) {
                return 2;
            }
            try (Client client = new Client(config)) {
                FileResponse meta = client.get(
                        "/api/files/" + fileId,
                        FileResponse.class,
                        Map.of("spaceId", String.valueOf(sid)));
                Path target = (output != null && Files.isDirectory(output))
                        ? output.resolve(meta.getOriginalName() == null
                                ? ("file-" + fileId)
                                : meta.getOriginalName())
                        : output;
                client.download("/api/files/" + fileId + "/content?spaceId=" + sid, target);
                System.out.println("Downloaded to " + target);
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }
    }

    /**
     * 查看文件元信息。
     */
    @Command(name = "info", mixinStandardHelpOptions = true, description = "Show file metadata")
    public static class InfoCommand extends FileSubcommand {

        @Parameters(index = "0", description = "File ID")
        Long fileId;

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = SpacesCommand.requireAuth(printer);
            if (config == null) {
                return 2;
            }
            Long sid = requireSpaceId(config, printer);
            if (sid == null) {
                return 2;
            }
            try (Client client = new Client(config)) {
                FileResponse resp = client.get(
                        "/api/files/" + fileId,
                        FileResponse.class,
                        Map.of("spaceId", String.valueOf(sid)));
                if (Printer.isJson()) {
                    printer.printObject(resp);
                } else {
                    Map<String, Object> kv = new LinkedHashMap<>();
                    kv.put("id", resp.getId());
                    kv.put("name", resp.getOriginalName());
                    kv.put("contentType", resp.getContentType());
                    kv.put("size", humanSize(resp.getSize()));
                    kv.put("visibility", resp.getVisibility());
                    kv.put("createdAt", resp.getCreatedAt());
                    kv.put("updatedAt", resp.getUpdatedAt());
                    kv.put("createdBy", resp.getCreatedBy());
                    kv.put("accessUrl", resp.getAccessUrl());
                    kv.forEach((k, v) -> System.out.println(k + ": " + v));
                }
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }
    }

    /**
     * 打印访问链接。
     */
    @Command(name = "link", mixinStandardHelpOptions = true, description = "Print the access URL for a file")
    public static class LinkCommand extends FileSubcommand {

        @Parameters(index = "0", description = "File ID")
        Long fileId;

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = SpacesCommand.requireAuth(printer);
            if (config == null) {
                return 2;
            }
            Long sid = requireSpaceId(config, printer);
            if (sid == null) {
                return 2;
            }
            try (Client client = new Client(config)) {
                FileResponse resp = client.get(
                        "/api/files/" + fileId,
                        FileResponse.class,
                        Map.of("spaceId", String.valueOf(sid)));
                String base = config.getServerUrl();
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
                String url = base + (resp.getAccessUrl() == null ? "" : resp.getAccessUrl());
                if (Printer.isJson()) {
                    printer.printObject(new LinkPayload(url, resp.getVisibility()));
                } else {
                    System.out.println(url);
                }
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class LinkPayload {
            private final String url;
            private final String visibility;
        }
    }

    /**
     * 设为 PUBLIC。
     */
    @Command(name = "share", mixinStandardHelpOptions = true, description = "Set file visibility to PUBLIC")
    public static class ShareCommand extends FileSubcommand {

        @Parameters(index = "0", description = "File ID")
        Long fileId;

        @Override
        public Integer call() throws Exception {
            return runVisibilityChange(fileId, "PUBLIC");
        }
    }

    /**
     * 设为 PRIVATE。
     */
    @Command(name = "private", mixinStandardHelpOptions = true, description = "Set file visibility to PRIVATE")
    public static class PrivateCommand extends FileSubcommand {

        @Parameters(index = "0", description = "File ID")
        Long fileId;

        @Override
        public Integer call() throws Exception {
            return runVisibilityChange(fileId, "PRIVATE");
        }
    }

    /**
     * 删除文件。
     */
    @Command(name = "delete", mixinStandardHelpOptions = true, description = "Delete a file")
    public static class DeleteCommand extends FileSubcommand {

        @Parameters(index = "0", description = "File ID")
        Long fileId;

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = SpacesCommand.requireAuth(printer);
            if (config == null) {
                return 2;
            }
            Long sid = requireSpaceId(config, printer);
            if (sid == null) {
                return 2;
            }
            try (Client client = new Client(config)) {
                client.delete("/api/files/" + fileId, Map.of("spaceId", String.valueOf(sid)));
                System.out.println("Deleted file " + fileId);
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }
    }

    static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ShareResult {
        private final String url;
        private final String visibility;
    }
}