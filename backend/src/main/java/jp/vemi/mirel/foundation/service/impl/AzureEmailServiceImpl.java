/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service.impl;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.SyncPoller;
import jp.vemi.mirel.foundation.service.EmailService;
import jp.vemi.mirel.foundation.service.EmailTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Azure Communication Services メール送信サービス実装.
 * 本番環境向け
 */
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "azure")
@Slf4j
public class AzureEmailServiceImpl implements EmailService {
    
    private final EmailClient emailClient;
    private final EmailTemplateService templateService;
    private final String senderAddress;
    
    public AzureEmailServiceImpl(
        @Value("${email.azure.communication.connection-string}") String connectionString,
        @Value("${email.azure.communication.email.from}") String senderAddress,
        EmailTemplateService templateService
    ) {
        this.emailClient = new EmailClientBuilder()
            .connectionString(connectionString)
            .buildClient();
        this.senderAddress = senderAddress;
        this.templateService = templateService;
        log.info("Azure Email Service初期化完了: sender={}", senderAddress);
    }
    
    @Override
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        String htmlContent = templateService.processTemplate(templateName, variables);
        sendHtmlEmail(to, subject, htmlContent);
    }
    
    @Override
    public void sendPlainTextEmail(String to, String subject, String body) {
        try {
            EmailMessage message = new EmailMessage()
                .setSenderAddress(senderAddress)
                .setToRecipients(new EmailAddress(to))
                .setSubject(subject)
                .setBodyPlainText(body);
            
            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(message);
            EmailSendResult result = poller.getFinalResult();
            
            log.info("Azure プレーンテキストメール送信成功: to={}, messageId={}", to, result.getId());
        } catch (Exception e) {
            log.error("Azure プレーンテキストメール送信失敗: to={}, error={}", to, e.getMessage(), e);
            // システムエラーの詳細は隠蔽し、一般的なメッセージのみスロー
            throw new RuntimeException("メール送信に失敗しました");
        }
    }
    
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            EmailMessage message = new EmailMessage()
                .setSenderAddress(senderAddress)
                .setToRecipients(new EmailAddress(to))
                .setSubject(subject)
                .setBodyHtml(htmlBody);
            
            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(message);
            EmailSendResult result = poller.getFinalResult();
            
            log.info("Azure HTMLメール送信成功: to={}, messageId={}", to, result.getId());
        } catch (Exception e) {
            log.error("Azure HTMLメール送信失敗: to={}, error={}", to, e.getMessage(), e);
            // システムエラーの詳細は隠蔽し、一般的なメッセージのみスロー
            throw new RuntimeException("メール送信に失敗しました");
        }
    }
}
