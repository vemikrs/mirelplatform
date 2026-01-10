/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jp.vemi.framework.util.SanitizeUtil;
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

        log.warn("Validation error on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

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

        log.warn("Constraint violation on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

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

        log.warn("Access denied on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

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

        log.warn("Email not verified on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

        Map<String, Object> data = Map.of(
                "email", ex.getEmail(),
                "errorCode", "EMAIL_NOT_VERIFIED");

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

        log.warn("Illegal argument on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of(ex.getMessage() != null ? ex.getMessage() : "不正なリクエストです"))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * リソースが見つからないエラー (404 Not Found)
     */
    @ExceptionHandler(jp.vemi.framework.exeption.MirelResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            jp.vemi.framework.exeption.MirelResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of(ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * クォータ超過/レート制限エラー (429 Too Many Requests)
     */
    @ExceptionHandler(jp.vemi.framework.exeption.MirelQuotaExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleQuotaExceededException(
            jp.vemi.framework.exeption.MirelQuotaExceededException ex, WebRequest request) {

        log.warn("Quota exceeded on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of(ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * バリデーションエラー/業務ルール違反 (400 Bad Request)
     */
    @ExceptionHandler(jp.vemi.framework.exeption.MirelValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMirelValidationException(
            jp.vemi.framework.exeption.MirelValidationException ex, WebRequest request) {

        log.warn("Business validation error on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of(ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * その他のアプリケーション例外 (400 Bad Request)
     */
    @ExceptionHandler(jp.vemi.framework.exeption.MirelApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMirelApplicationException(
            jp.vemi.framework.exeption.MirelApplicationException ex, WebRequest request) {

        log.warn("Application error on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()));

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of(ex.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * システム固有例外 (500 Internal Server Error)
     * クライアントには詳細を隠蔽します。
     */
    @ExceptionHandler(jp.vemi.framework.exeption.MirelSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleMirelSystemException(
            jp.vemi.framework.exeption.MirelSystemException ex, WebRequest request) {

        log.error("System exception on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of("システムエラーが発生しました。管理者に連絡してください。"))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * RuntimeException (予期せぬエラーとみなす)
     * 以前は400を返していましたが、今後はシステムエラーとして500を返し、詳細は隠蔽します。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("Unexpected runtime exception on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of("システムエラーが発生しました。管理者に連絡してください。"))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * その他のすべての例外 (システムエラー)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected error on {}: {}", SanitizeUtil.forLog(request.getDescription(false)),
                SanitizeUtil.forLog(ex.getMessage()), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .errors(List.of("システムエラーが発生しました。管理者に連絡してください。"))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
