/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.exception;

/**
 * メールアドレス未検証例外.
 * ユーザーのメールアドレスが未検証の状態でログインしようとした場合にスローされる
 */
public class EmailNotVerifiedException extends RuntimeException {
    
    private final String email;
    
    /**
     * コンストラクタ
     * 
     * @param message エラーメッセージ
     * @param email 未検証のメールアドレス
     */
    public EmailNotVerifiedException(String message, String email) {
        super(message);
        this.email = email;
    }
    
    /**
     * 未検証のメールアドレスを取得
     * 
     * @return メールアドレス
     */
    public String getEmail() {
        return email;
    }
}
