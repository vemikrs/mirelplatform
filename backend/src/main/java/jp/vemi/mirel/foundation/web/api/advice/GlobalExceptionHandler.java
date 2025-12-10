/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.advice;

import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * アプリケーション全体の例外ハンドリング.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 未処理の例外をハンドリングする.
     * 
     * @param e
     *            例外
     * @return エラーレスポンス
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        // ERRORレベルでログ出力 (これにより OneFilePerExceptionAppender が反応する設定を行う)
        log.error("Unhandled exception occurred: {}", e.getMessage(), e);

        ApiResponse<Object> response = ApiResponse.builder()
                .errors(List.of("システムエラーが発生しました: " + e.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // RuntimeException も明示的に定義しておくと無難だが、Exception でキャッチされるため必須ではない。
    // 将来的には特定のApplicationExceptionに対する個別ハンドリングを追加する場所となる。
}
