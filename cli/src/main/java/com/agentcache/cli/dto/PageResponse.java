package com.agentcache.cli.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页响应 DTO（镜像后端 {@code PageResponse}）。
 */
@Data
public class PageResponse<T> {

    private List<T> content;
    private long total;
    private int page;
    private int size;
}