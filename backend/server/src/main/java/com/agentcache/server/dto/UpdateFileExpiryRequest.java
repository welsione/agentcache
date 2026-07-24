package com.agentcache.server.dto;

import lombok.Data;

/**
 * 修改文件有效期请求 DTO。
 */
@Data
public class UpdateFileExpiryRequest {

    /** 有效期小时数，null 表示永久 */
    private Integer expiresInHours;
}
