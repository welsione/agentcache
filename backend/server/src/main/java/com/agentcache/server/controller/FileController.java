package com.agentcache.server.controller;

import com.agentcache.application.service.ApiKeyService.AuthenticatedCaller;
import com.agentcache.application.service.FileAccessLogService;
import com.agentcache.application.service.FileService;
import com.agentcache.common.response.Result;
import com.agentcache.domain.entity.FileAccessLog;
import com.agentcache.domain.entity.FileRecord;
import com.agentcache.domain.enums.ActorType;
import com.agentcache.domain.enums.FileAccessAction;
import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.server.dto.FileAccessLogResponse;
import com.agentcache.server.dto.FileResponse;
import com.agentcache.server.dto.PageResponse;
import com.agentcache.server.dto.UpdateFileDescriptionRequest;
import com.agentcache.server.dto.UpdateFileExpiryRequest;
import com.agentcache.server.dto.VisibilityUpdateRequest;
import com.agentcache.server.security.AuthenticatedActor;
import com.agentcache.server.security.RequestActorResolver;
import com.agentcache.server.support.FileDownloadSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件控制器。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileAccessLogService accessLogService;
    private final RequestActorResolver actorResolver;

    @GetMapping("/spaces/{spaceId}/files")
    public Result<PageResponse<FileResponse>> list(@PathVariable Long spaceId,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        Page<FileRecord> records = fileService.list(spaceId, caller, q, PageRequest.of(page, size));
        return Result.success(PageResponse.from(records, FileResponse::from));
    }

    @PostMapping(value = "/spaces/{spaceId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileResponse> upload(@PathVariable Long spaceId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "description", required = false) String description,
                                       @RequestParam(value = "visibility", required = false) FileVisibility visibility,
                                       @RequestParam(value = "expiresInHours", required = false) Integer expiresInHours) throws IOException {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        FileRecord record;
        try (InputStream in = file.getInputStream()) {
            record = fileService.upload(spaceId, caller, file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), in,
                    description, visibility, expiresInHours);
        }
        return Result.success(FileResponse.from(record));
    }

    @GetMapping("/files/{fileId}")
    public Result<FileResponse> get(@PathVariable Long fileId,
                                    @RequestParam Long spaceId) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(FileResponse.from(fileService.get(spaceId, fileId, caller)));
    }

    @GetMapping("/files/{fileId}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId,
                                                       @RequestParam Long spaceId) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        FileRecord record = fileService.get(spaceId, fileId, caller);
        InputStream stream = fileService.download(spaceId, fileId, caller);
        return FileDownloadSupport.build(record, stream);
    }

    @PutMapping("/files/{fileId}/visibility")
    public Result<FileResponse> setVisibility(@PathVariable Long fileId,
                                              @RequestParam Long spaceId,
                                              @Valid @RequestBody VisibilityUpdateRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        FileRecord record = fileService.setVisibility(spaceId, fileId, request.getVisibility(), caller);
        return Result.success(FileResponse.from(record));
    }

    /**
     * 修改文件说明。
     */
    @PutMapping("/files/{fileId}/description")
    public Result<FileResponse> updateDescription(@PathVariable Long fileId,
                                                   @RequestParam Long spaceId,
                                                   @Valid @RequestBody UpdateFileDescriptionRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(FileResponse.from(
                fileService.updateDescription(spaceId, fileId, request.getDescription(), caller)));
    }

    /**
     * 修改文件有效期。
     */
    @PutMapping("/files/{fileId}/expiry")
    public Result<FileResponse> updateExpiry(@PathVariable Long fileId,
                                              @RequestParam Long spaceId,
                                              @Valid @RequestBody UpdateFileExpiryRequest request) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        return Result.success(FileResponse.from(
                fileService.updateExpiry(spaceId, fileId, request.getExpiresInHours(), caller)));
    }

    @DeleteMapping("/files/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result<Void> delete(@PathVariable Long fileId,
                               @RequestParam Long spaceId) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        fileService.delete(spaceId, fileId, caller);
        return Result.success();
    }

    /**
     * 查询空间文件访问日志。
     */
    @GetMapping("/spaces/{spaceId}/access-logs")
    public Result<PageResponse<FileAccessLogResponse>> listAccessLogs(
            @PathVariable Long spaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AuthenticatedCaller caller = toCaller(actorResolver.require());
        // 验证空间访问权限
        fileService.requireSpaceAccess(spaceId, caller);
        Page<FileAccessLog> logs = accessLogService.listBySpace(spaceId, PageRequest.of(page, size));
        return Result.success(PageResponse.from(logs, FileAccessLogResponse::from));
    }

    static AuthenticatedCaller toCaller(AuthenticatedActor actor) {
        return new AuthenticatedCaller(actor.getUserId(), actor.getKind() == AuthenticatedActor.Kind.API_KEY,
                actor.getSpaceId(), actor.getSpaceRole());
    }
}
