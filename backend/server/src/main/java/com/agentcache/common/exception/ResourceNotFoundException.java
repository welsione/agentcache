package com.agentcache.common.exception;

/**
 * 资源不存在异常。
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
