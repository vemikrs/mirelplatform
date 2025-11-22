/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service;

import java.util.Map;

/**
 * メール送信サービスインターフェース.
 * 開発環境SMTP・本番Azure Communication Servicesの抽象化
 */
public interface EmailService {
    
    /**
     * テンプレートメールを送信
     * 
     * @param to 送信先メールアドレス
     * @param subject 件名
     * @param templateName テンプレート名 (例: "otp-login")
     * @param variables テンプレート変数
     */
    void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables);
    
    /**
     * プレーンテキストメールを送信
     * 
     * @param to 送信先メールアドレス
     * @param subject 件名
     * @param body 本文
     */
    void sendPlainTextEmail(String to, String subject, String body);
    
    /**
     * HTMLメールを送信
     * 
     * @param to 送信先メールアドレス
     * @param subject 件名
     * @param htmlBody HTML本文
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);
}
