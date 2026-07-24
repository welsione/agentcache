package com.agentcache.application.dto;

import com.agentcache.domain.enums.FileVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 上传文件结果。
 */
@Data
@AllArgsConstructor
public class UploadFileResult {

    private Long id;
    private String originalName;
    private Long size;
    private FileVisibility visibility;
    private String accessUrl;
}
