package com.agentcache.domain.repository;

import com.agentcache.domain.entity.FileRecord;
import com.agentcache.domain.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件记录数据访问接口。
 */
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    Page<FileRecord> findBySpaceIdAndStatus(Long spaceId, EntityStatus status, Pageable pageable);

    Page<FileRecord> findBySpaceIdAndOriginalNameContainingAndStatus(
            Long spaceId, String keyword, EntityStatus status, Pageable pageable);

    Optional<FileRecord> findByIdAndSpaceId(Long id, Long spaceId);

    /**
     * 查询已过期但仍为 ACTIVE 状态的文件。
     */
    List<FileRecord> findByExpiresAtBeforeAndStatus(LocalDateTime expiresAt, EntityStatus status);
}
