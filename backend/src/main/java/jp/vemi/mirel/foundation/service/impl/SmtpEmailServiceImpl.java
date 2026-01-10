/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service.impl;

import jp.vemi.mirel.foundation.service.EmailService;
import jp.vemi.mirel.foundation.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jp.vemi.framework.util.SanitizeUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * SMTP メール送信サービス実装.
 * 開発環境・MailHog向け
 */
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Override
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        String htmlContent = templateService.processTemplate(templateName, variables);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendPlainTextEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@mirelplatform.local");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("プレーンテキストメール送信成功: to={}, subject={}", SanitizeUtil.forLog(to), SanitizeUtil.forLog(subject));
        } catch (Exception e) {
            log.error("プレーンテキストメール送信失敗: to={}, error={}", SanitizeUtil.forLog(to), SanitizeUtil.forLog(e.getMessage()),
                    e);
            // システムエラーの詳細は隠蔽し、一般的なメッセージのみスロー
            throw new RuntimeException("メール送信に失敗しました");
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@mirelplatform.local");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTMLメール送信成功: to={}, subject={}", SanitizeUtil.forLog(to), SanitizeUtil.forLog(subject));
        } catch (MessagingException e) {
            log.error("HTMLメール送信失敗: to={}, error={}", SanitizeUtil.forLog(to), SanitizeUtil.forLog(e.getMessage()), e);
            // システムエラーの詳細は隠蔽し、一般的なメッセージのみスロー
            throw new RuntimeException("メール送信に失敗しました");
        }
    }
}
