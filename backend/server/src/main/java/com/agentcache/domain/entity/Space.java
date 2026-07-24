package com.agentcache.domain.entity;

import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.FileVisibility;
import com.agentcache.domain.enums.StorageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 空间实体。
 */
@Entity
@Table(name = "space")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EntityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StorageType storageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FileVisibility defaultVisibility;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        updatedAt = TimeUtil.now();
        if (status == null) {
            status = EntityStatus.ACTIVE;
        }
        if (storageType == null) {
            storageType = StorageType.LOCAL;
        }
        if (defaultVisibility == null) {
            defaultVisibility = FileVisibility.PRIVATE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeUtil.now();
    }
}
