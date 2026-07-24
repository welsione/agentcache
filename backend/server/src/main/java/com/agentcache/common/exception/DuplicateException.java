package com.agentcache.common.exception;

/**
 * 资源重复异常。
 */
public class DuplicateException extends DomainException {

    public DuplicateException(String message) {
        super(message);
    }
}
