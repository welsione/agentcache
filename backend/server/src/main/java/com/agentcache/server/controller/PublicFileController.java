package com.agentcache.server.controller;

import com.agentcache.application.service.FileAccessLogService;
import com.agentcache.application.service.FileService;
import com.agentcache.common.response.Result;
import com.agentcache.domain.entity.FileRecord;
import com.agentcache.domain.enums.ActorType;
import com.agentcache.domain.enums.FileAccessAction;
import com.agentcache.server.dto.FileResponse;
import com.agentcache.server.support.FileDownloadSupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

/**
 * 公开文件访问控制器，无需登录。
 */
@RestController
@RequestMapping("/public/files")
@RequiredArgsConstructor
public class PublicFileController {

    private final FileService fileService;
    private final FileAccessLogService accessLogService;

    /**
     * 返回 PUBLIC 文件的元数据。
     */
    @GetMapping("/{fileId}")
    public Result<FileResponse> metadata(@PathVariable Long fileId, HttpServletRequest request) {
        FileRecord record = fileService.getPublicFile(fileId);
        FileResponse response = FileResponse.from(record);
        response.setAccessUrl("/public/files/" + record.getId() + "/content");

        // 记录匿名查看日志
        accessLogService.log(fileId, record.getSpaceId(), FileAccessAction.VIEW,
                ActorType.ANONYMOUS, null, null, request, null);

        return Result.success(response);
    }

    /**
     * 下载 PUBLIC 文件内容。
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId, HttpServletRequest request) {
        FileRecord record = fileService.getPublicFile(fileId);

        // 记录匿名下载日志
        accessLogService.log(fileId, record.getSpaceId(), FileAccessAction.DOWNLOAD,
                ActorType.ANONYMOUS, null, null, request, null);

        InputStream stream = fileService.download(record.getSpaceId(), fileId, null);
        return FileDownloadSupport.build(record, stream);
    }
}
