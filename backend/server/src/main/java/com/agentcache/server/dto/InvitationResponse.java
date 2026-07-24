package com.agentcache.server.dto;

import com.agentcache.domain.entity.InvitationToken;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请令牌响应。
 */
@Data
public class InvitationResponse {

    private Long id;
    private String token;
    private String inviteUrl;
    private Long createdBy;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    /**
     * 从实体转换为响应 DTO。
     */
    public static InvitationResponse from(InvitationToken invitation) {
        InvitationResponse response = new InvitationResponse();
        response.setId(invitation.getId());
        response.setToken(invitation.getToken());
        response.setInviteUrl("/invite/" + invitation.getToken());
        response.setCreatedBy(invitation.getCreatedBy());
        response.setUsedAt(invitation.getUsedAt());
        response.setExpiresAt(invitation.getExpiresAt());
        response.setCreatedAt(invitation.getCreatedAt());
        return response;
    }
}
