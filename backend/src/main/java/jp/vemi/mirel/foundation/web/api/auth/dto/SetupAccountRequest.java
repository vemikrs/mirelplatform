/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * アカウントセットアップリクエスト
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupAccountRequest {
    
    /**
     * セットアップトークン
     */
    @NotBlank(message = "セットアップトークンは必須です")
    private String token;
    
    /**
     * 新しいパスワード
     */
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上である必要があります")
    private String newPassword;
}
