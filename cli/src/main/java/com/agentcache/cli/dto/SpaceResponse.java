package com.agentcache.cli.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间响应 DTO（镜像后端 {@code SpaceResponse}）。
 */
@Data
public class SpaceResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private LocalDateTime createdAt;
}