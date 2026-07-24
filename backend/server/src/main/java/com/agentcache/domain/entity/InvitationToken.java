package com.agentcache.domain.entity;

import com.agentcache.common.util.TimeUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 邀请令牌实体。
 */
@Entity
@Table(name = "invitationToken")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private Long createdBy;

    @Column
    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
    }
}
