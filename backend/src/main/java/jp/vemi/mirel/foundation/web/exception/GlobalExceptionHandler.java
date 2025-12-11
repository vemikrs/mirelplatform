/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jp.vemi.mirel.foundation.exception.EmailNotVerifiedException;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * グローバルエラーハンドラ.
 * すべてのREST APIエラーを一元的に処理し、クライアントへのエラー情報漏洩を防止します。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * バリデーションエラー (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.warn("Validation error on {}: {}", request.getDescription(false), ex.getMessage());
        
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 制約違反エラー (Jakarta Validation)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        log.warn("Constraint violation on {}: {}", request.getDescription(false), ex.getMessage());
        
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * アクセス拒否エラー
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Access denied on {}: {}", request.getDescription(false), ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of("アクセスが拒否されました"))
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * メールアドレス未検証エラー
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleEmailNotVerified(
            EmailNotVerifiedException ex, WebRequest request) {
        
        log.warn("Email not verified on {}: {}", request.getDescription(false), ex.getMessage());
        
        Map<String, Object> data = Map.of(
                "email", ex.getEmail(),
                "errorCode", "EMAIL_NOT_VERIFIED"
        );
        
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .data(data)
                .errors(List.of(ex.getMessage()))
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 不正引数エラー
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Illegal argument on {}: {}", request.getDescription(false), ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of(ex.getMessage() != null ? ex.getMessage() : "不正なリクエストです"))
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * RuntimeException (ビジネスロジックエラー)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        log.error("Runtime exception on {}: {}", request.getDescription(false), ex.getMessage(), ex);
        
        // RuntimeExceptionのメッセージは業務エラーとみなし、クライアントに返す
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of(ex.getMessage() != null ? ex.getMessage() : "処理中にエラーが発生しました"))
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * その他のすべての例外 (システムエラー)
     * システムエラーの詳細はログに記録し、クライアントには一般的なメッセージのみ返します。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error on {}: {}", request.getDescription(false), ex.getMessage(), ex);
        
        // システムエラーの詳細は隠蔽し、一般的なメッセージのみ返す
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of("システムエラーが発生しました。管理者に連絡してください。"))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
