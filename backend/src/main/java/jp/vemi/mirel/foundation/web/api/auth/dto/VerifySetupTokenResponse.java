/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * アカウントセットアップトークン検証レスポンス
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifySetupTokenResponse {
    
    /**
     * メールアドレス
     */
    private String email;
    
    /**
     * ユーザー名
     */
    private String username;
}
