package com.agentcache.domain.entity;

import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.enums.SpaceMemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 空间成员实体。
 */
@Entity
@Table(name = "spaceMember")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long spaceId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SpaceMemberRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        if (role == null) {
            role = SpaceMemberRole.MEMBER;
        }
    }
}
