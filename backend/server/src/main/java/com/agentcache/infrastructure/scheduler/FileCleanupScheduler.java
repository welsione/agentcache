package com.agentcache.infrastructure.scheduler;

import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.entity.FileRecord;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.StorageType;
import com.agentcache.domain.port.FileStoragePort;
import com.agentcache.domain.repository.FileRecordRepository;
import com.agentcache.infrastructure.storage.StorageRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件过期清除定时任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final FileRecordRepository fileRecordRepository;
    private final StorageRouter storageRouter;

    /**
     * 每小时扫描过期文件并清除。
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredFiles() {
        LocalDateTime now = TimeUtil.now();
        List<FileRecord> expired = fileRecordRepository
                .findByExpiresAtBeforeAndStatus(now, EntityStatus.ACTIVE);
        for (FileRecord record : expired) {
            try {
                record.setStatus(EntityStatus.DELETED);
                fileRecordRepository.save(record);
                StorageType type = record.getStorageType() != null ? record.getStorageType() : StorageType.LOCAL;
                storageRouter.resolve(type).delete(record.getStoragePath());
                log.info("Cleaned up expired file: {} (expired at {})", record.getId(), record.getExpiresAt());
            } catch (Exception e) {
                log.error("Failed to cleanup expired file: {}", record.getId(), e);
            }
        }
        if (!expired.isEmpty()) {
            log.info("Cleaned up {} expired files", expired.size());
        }
    }
}
