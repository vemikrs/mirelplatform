/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.bootstrap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初期管理者作成リクエスト.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInitialAdminRequest {

    /**
     * Bootstrapトークン
     * ストレージに生成されたトークンファイルから取得
     */
    @NotBlank(message = "トークンは必須です")
    private String token;

    /**
     * ユーザー名
     */
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 3, max = 50, message = "ユーザー名は3文字以上50文字以内で入力してください")
    private String username;

    /**
     * メールアドレス
     */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    /**
     * パスワード
     */
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください")
    private String password;

    /**
     * 表示名
     */
    private String displayName;
}
