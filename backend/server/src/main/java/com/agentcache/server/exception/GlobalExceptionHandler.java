package com.agentcache.server.exception;

import com.agentcache.common.exception.AuthenticationException;
import com.agentcache.common.exception.DomainException;
import com.agentcache.common.exception.DuplicateException;
import com.agentcache.common.exception.ResourceNotFoundException;
import com.agentcache.common.exception.UnauthorizedException;
import com.agentcache.common.exception.ValidationException;
import com.agentcache.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return Result.error(404, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthentication(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return Result.error(401, e.getMessage());
    }

    @ExceptionHandler({ValidationException.class, DuplicateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadRequest(DomainException e) {
        log.warn("Bad request: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleUnauthorized(UnauthorizedException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return Result.error(403, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        log.warn("Validation failed: {}", detail);
        return Result.error(400, detail);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("Bad request: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理不支持的 HTTP 方法,返回 405。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {} {}", e.getMethod(), e.getMessage());
        return Result.error(405, "Request method '" + e.getMethod() + "' is not supported");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Internal error", e);
        return Result.error(500, "Internal server error");
    }
}