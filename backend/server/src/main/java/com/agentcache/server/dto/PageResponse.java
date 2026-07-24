package com.agentcache.server.dto;

import lombok.Data;

/**
 * 分页响应包装。
 */
@Data
public class PageResponse<T> {

    private java.util.List<T> content;
    private long total;
    private int page;
    private int size;

    public PageResponse() {
    }

    public PageResponse(java.util.List<T> content, long total, int page, int size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <S, T> PageResponse<T> from(org.springframework.data.domain.Page<S> source,
                                                java.util.function.Function<S, T> mapper) {
        java.util.List<T> mapped = source.getContent().stream().map(mapper).toList();
        return new PageResponse<>(mapped, source.getTotalElements(), source.getNumber(), source.getSize());
    }
}