package com.agentcache.domain.entity;

import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.enums.UserRole;
import com.agentcache.domain.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Entity
@Table(name = "`user`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EntityStatus status;

    @Column(nullable = false)
    private Boolean mustChangePassword;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
        updatedAt = TimeUtil.now();
        if (role == null) {
            role = UserRole.USER;
        }
        if (status == null) {
            status = EntityStatus.ACTIVE;
        }
        if (mustChangePassword == null) {
            mustChangePassword = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = TimeUtil.now();
    }
}
