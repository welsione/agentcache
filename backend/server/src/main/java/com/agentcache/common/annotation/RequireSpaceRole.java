package com.agentcache.common.annotation;

import com.agentcache.domain.enums.SpaceMemberRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要指定空间角色的方法。
 *
 * <p>切面从方法参数中提取 spaceId(通过 {@link SpaceId} 注解或名为 "spaceId"/"id" 的参数),
 * 然后校验当前用户在该空间的角色是否满足要求。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSpaceRole {

    /** 最低要求的空间角色。 */
    SpaceMemberRole value();
}
