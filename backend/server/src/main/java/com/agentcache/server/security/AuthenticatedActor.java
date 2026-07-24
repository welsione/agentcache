package com.agentcache.server.security;

import com.agentcache.domain.enums.SpaceMemberRole;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 已认证的请求主体。
 *
 * <p>统一表达 JWT 用户与 API Key 两种身份。控制器与服务层通过该对象获取身份信息，
 * 不再直接处理 {@code Authentication} 中的具体类型。</p>
 */
@Getter
@Setter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class AuthenticatedActor {

    /** 用户 ID，JWT 用户场景下非空。 */
    private Long userId;

    /** 用户名，仅 JWT 场景下设置。 */
    private String username;

    /** 用户全局角色，仅 JWT 场景下设置。 */
    private String userRole;

    /** API Key ID，API Key 场景下非空。 */
    private Long apiKeyId;

    /** API Key 绑定的空间 ID，仅 API Key 场景下设置。 */
    private Long spaceId;

    /** API Key 在所属空间中的角色，仅 API Key 场景下设置。 */
    private SpaceMemberRole spaceRole;

    /** 当前认证方式。 */
    private Kind kind;

    public enum Kind {
        USER,
        API_KEY
    }
}