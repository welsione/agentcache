package com.agentcache.domain.repository;

import com.agentcache.domain.entity.FileAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 文件访问日志 Repository。
 */
public interface FileAccessLogRepository extends JpaRepository<FileAccessLog, Long> {

    /**
     * 按空间查询访问日志，按时间倒序。
     */
    Page<FileAccessLog> findBySpaceIdOrderByCreatedAtDesc(Long spaceId, Pageable pageable);

    /**
     * 按文件查询访问日志，按时间倒序。
     */
    Page<FileAccessLog> findByFileIdOrderByCreatedAtDesc(Long fileId, Pageable pageable);
}
