package com.agentcache.domain.entity;

import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.enums.EntityStatus;
import com.agentcache.domain.enums.SpaceMemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * API Key 实体。
 */
@Entity
@Table(name = "apiKey")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long spaceId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 256)
    private String keyHash;

    @Column(nullable = false, length = 16)
    private String keyPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SpaceMemberRole role;

    @Column
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime lastUsedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EntityStatus status;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        if (status == null) {
            status = EntityStatus.ACTIVE;
        }
    }
}
