package com.agentcache.common.exception;

/**
 * 参数校验异常。
 */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
