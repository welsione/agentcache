package com.agentcache.domain.entity;

import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.StorageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 文件记录实体。
 */
@Entity
@Table(name = "fileRecord")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long spaceId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(nullable = false, length = 256)
    private String originalName;

    @Column(length = 128)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 512)
    private String storagePath;

    @Column(length = 128)
    private String checksum;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FileVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private StorageType storageType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EntityStatus status;

    @Column
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        updatedAt = TimeUtil.now();
        if (version == null) {
            version = 1;
        }
        if (visibility == null) {
            visibility = FileVisibility.PRIVATE;
        }
        if (status == null) {
            status = EntityStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeUtil.now();
    }
}
