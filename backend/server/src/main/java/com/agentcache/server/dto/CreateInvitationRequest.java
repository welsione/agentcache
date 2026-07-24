package com.agentcache.server.dto;

import lombok.Data;

/**
 * 创建邀请令牌请求。
 */
@Data
public class CreateInvitationRequest {

    /** 过期时间(小时),默认 72。 */
    private Integer expiresInHours;
}
