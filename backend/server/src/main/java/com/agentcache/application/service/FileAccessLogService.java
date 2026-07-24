package com.agentcache.application.service;

import com.agentcache.domain.entity.FileAccessLog;
import com.agentcache.domain.enums.ActorType;
import com.agentcache.domain.enums.FileAccessAction;
import com.agentcache.domain.repository.FileAccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 文件访问日志服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileAccessLogService {

    private final FileAccessLogRepository repository;

    /**
     * 记录文件访问日志。
     *
     * @param fileId    文件 ID
     * @param spaceId   空间 ID
     * @param action    操作类型
     * @param actorType 操作者类型
     * @param actorId   操作者 ID
     * @param actorName 操作者名称
     * @param request   HTTP 请求（可为 null）
     * @param details   详情
     */
    public void log(Long fileId, Long spaceId, FileAccessAction action,
                    ActorType actorType, Long actorId, String actorName,
                    HttpServletRequest request, String details) {
        try {
            FileAccessLog logEntry = FileAccessLog.builder()
                    .fileId(fileId)
                    .spaceId(spaceId)
                    .action(action)
                    .actorType(actorType)
                    .actorId(actorId)
                    .actorName(actorName)
                    .ip(getClientIp(request))
                    .userAgent(request != null ? truncate(request.getHeader("User-Agent"), 512) : null)
                    .details(truncate(details, 1024))
                    .build();
            repository.save(logEntry);
        } catch (Exception e) {
            // 审计日志不应影响主流程
            log.warn("Failed to log file access: fileId={}, action={}", fileId, action, e);
        }
    }

    /**
     * 分页查询空间访问日志。
     */
    public Page<FileAccessLog> listBySpace(Long spaceId, Pageable pageable) {
        return repository.findBySpaceIdOrderByCreatedAtDesc(spaceId, pageable);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
