package com.example.auth.exception;

import com.example.auth.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(e.getCode()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 优先取 NotBlank 类型的错误（如"不能为空"），再取其他校验错误
        String message = e.getBindingResult().getFieldErrors().stream()
                .filter(error -> error.getCode() != null && error.getCode().contains("NotBlank"))
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElseGet(() -> e.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .findFirst()
                        .orElse("参数校验失败"));
        ErrorResponse error = ErrorResponse.builder()
                .code(400)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(401)
                .message("认证失败：" + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(403)
                .message("没有访问权限")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse error = ErrorResponse.builder()
                .code(500)
                .message("服务器内部错误")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
