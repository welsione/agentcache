package com.agentcache.common.exception;

/**
 * 认证异常：用户名/密码错误、未激活等场景。
 */
public class AuthenticationException extends DomainException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}