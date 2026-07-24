package com.agentcache.cli;

import lombok.Getter;

/**
 * CLI 业务异常，对应后端 {@code Result.code != 200} 或 HTTP 错误。
 */
@Getter
public class CliException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public CliException(String message, int code) {
        this(message, code, 0);
    }

    public CliException(String message, int code, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public CliException(String message, int code, int httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}