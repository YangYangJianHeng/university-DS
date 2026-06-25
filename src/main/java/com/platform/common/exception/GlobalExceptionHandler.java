package com.platform.common.exception;

import com.platform.common.result.Result;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.badRequest(message);
    }

    @ExceptionHandler(ValidationException.class)
    public Result<?> handleValidationException(ValidationException e) {
        return Result.badRequest(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.badRequest("请求参数格式错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        return Result.forbidden("无权限访问");
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<?> handleSQLIntegrityConstraintViolation(SQLIntegrityConstraintViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return Result.conflict("数据冲突，操作无法完成");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("System exception", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
