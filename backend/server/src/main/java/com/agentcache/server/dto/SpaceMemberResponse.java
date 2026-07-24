package com.agentcache.server.dto;

import com.agentcache.domain.entity.SpaceMember;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间成员响应。
 */
@Data
public class SpaceMemberResponse {

    private Long id;
    private Long spaceId;
    private Long userId;
    private String role;
    private LocalDateTime createdAt;

    /**
     * 从实体转换为响应 DTO。
     */
    public static SpaceMemberResponse from(SpaceMember member) {
        SpaceMemberResponse response = new SpaceMemberResponse();
        response.setId(member.getId());
        response.setSpaceId(member.getSpaceId());
        response.setUserId(member.getUserId());
        response.setRole(member.getRole().name());
        response.setCreatedAt(member.getCreatedAt());
        return response;
    }
}
