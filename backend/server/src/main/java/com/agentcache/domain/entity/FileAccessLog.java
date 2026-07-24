package com.agentcache.domain.entity;

import com.agentcache.common.util.TimeUtil;
import com.agentcache.domain.enums.ActorType;
import com.agentcache.domain.enums.FileAccessAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 文件访问日志实体。
 */
@Entity
@Table(name = "fileAccessLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fileId;

    @Column(nullable = false)
    private Long spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FileAccessAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActorType actorType;

    @Column
    private Long actorId;

    @Column(length = 128)
    private String actorName;

    @Column(length = 64)
    private String ip;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 1024)
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = TimeUtil.now();
    }
}
