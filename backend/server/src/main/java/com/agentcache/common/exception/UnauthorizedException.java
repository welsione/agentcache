package com.agentcache.common.exception;

/**
 * 未授权异常。
 */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
