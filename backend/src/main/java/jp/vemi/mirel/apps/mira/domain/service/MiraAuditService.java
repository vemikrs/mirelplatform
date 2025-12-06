/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraAuditLogRepository;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira 監査サービス.
 * 
 * <p>AI アシスタント利用の監査ログを記録します。
 * 非同期で記録するため、メイン処理への影響を最小化します。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraAuditService {
    
    private final MiraAuditLogRepository auditLogRepository;
    private final MiraAiProperties aiProperties;
    
    /**
     * 監査ログを非同期で記録.
     *
     * @param builder 監査ログビルダー
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(AuditLogBuilder builder) {
        if (!isAuditEnabled()) {
            log.debug("監査ログは無効化されています");
            return;
        }
        
        try {
            MiraAuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
            log.debug("監査ログ記録: action={}, tenantId={}", 
                auditLog.getAction(), auditLog.getTenantId());
        } catch (Exception e) {
            log.error("監査ログ記録失敗", e);
        }
    }
    
    /**
     * 監査ログを同期で記録（トランザクション内で使用）.
     *
     * @param builder 監査ログビルダー
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSync(AuditLogBuilder builder) {
        if (!isAuditEnabled()) {
            return;
        }
        
        try {
            MiraAuditLog auditLog = builder.build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("監査ログ記録失敗", e);
        }
    }
    
    /**
     * チャット応答の監査ログを記録.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param conversationId 会話ID
     * @param mode モード
     * @param usedModel 使用モデル
     * @param latencyMs レイテンシ（ミリ秒）
     * @param promptLength プロンプト文字数
     * @param responseLength 応答文字数
     * @param status ステータス
     */
    public void logChatResponse(
            String tenantId,
            String userId,
            String conversationId,
            String mode,
            String usedModel,
            int latencyMs,
            Integer promptLength,
            Integer responseLength,
            MiraAuditLog.AuditStatus status) {
        
        logAsync(AuditLogBuilder.create()
            .action(MiraAuditLog.AuditAction.CHAT)
            .tenantId(tenantId)
            .userId(userId)
            .conversationId(conversationId)
            .mode(mode)
            .usedModel(usedModel)
            .latencyMs(latencyMs)
            .promptLength(promptLength)
            .responseLength(responseLength)
            .status(status));
    }
    
    /**
     * APIエラーの監査ログを記録.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param action アクション
     * @param errorMessage エラーメッセージ
     */
    public void logApiError(
            String tenantId,
            String userId,
            String action,
            String errorMessage) {
        
        logAsync(AuditLogBuilder.create()
            .action(MiraAuditLog.AuditAction.CHAT)
            .tenantId(tenantId)
            .userId(userId)
            .conversationId("error-" + UUID.randomUUID().toString())
            .status(MiraAuditLog.AuditStatus.ERROR)
            .errorCode(truncate(errorMessage, 50)));
    }
    
    /**
     * コンテキスト更新の監査ログを記録.
     *
     * @param userId ユーザーID
     * @param action アクション（例: USER_CONTEXT_UPDATED）
     */
    public void logContextUpdate(String userId, String action) {
        logAsync(AuditLogBuilder.create()
            .action(MiraAuditLog.AuditAction.CONTEXT_UPDATE)
            .userId(userId)
            .conversationId("context-" + UUID.randomUUID().toString())
            .status(MiraAuditLog.AuditStatus.SUCCESS));
    }
    
    /**
     * 監査ログが有効かどうか.
     */
    private boolean isAuditEnabled() {
        return aiProperties.getAudit() != null 
            && aiProperties.getAudit().isEnabled();
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    // ========================================
    // Builder Class
    // ========================================
    
    /**
     * 監査ログビルダー.
     */
    public static class AuditLogBuilder {
        private String tenantId;
        private String userId;
        private String conversationId;
        private String messageId;
        private MiraAuditLog.AuditAction action;
        private String mode;
        private String appId;
        private String screenId;
        private Integer promptLength;
        private Integer responseLength;
        private String usedModel;
        private Integer latencyMs;
        private MiraAuditLog.AuditStatus status;
        private String errorCode;
        
        public static AuditLogBuilder create() {
            return new AuditLogBuilder();
        }
        
        public AuditLogBuilder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public AuditLogBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public AuditLogBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }
        
        public AuditLogBuilder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }
        
        public AuditLogBuilder action(MiraAuditLog.AuditAction action) {
            this.action = action;
            return this;
        }
        
        public AuditLogBuilder mode(String mode) {
            this.mode = mode;
            return this;
        }
        
        public AuditLogBuilder appId(String appId) {
            this.appId = appId;
            return this;
        }
        
        public AuditLogBuilder screenId(String screenId) {
            this.screenId = screenId;
            return this;
        }
        
        public AuditLogBuilder promptLength(Integer promptLength) {
            this.promptLength = promptLength;
            return this;
        }
        
        public AuditLogBuilder responseLength(Integer responseLength) {
            this.responseLength = responseLength;
            return this;
        }
        
        public AuditLogBuilder usedModel(String usedModel) {
            this.usedModel = usedModel;
            return this;
        }
        
        public AuditLogBuilder latencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }
        
        public AuditLogBuilder status(MiraAuditLog.AuditStatus status) {
            this.status = status;
            return this;
        }
        
        public AuditLogBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public MiraAuditLog build() {
            return MiraAuditLog.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .userId(userId)
                .conversationId(conversationId)
                .messageId(messageId)
                .action(action != null ? action : MiraAuditLog.AuditAction.CHAT)
                .mode(mode)
                .appId(appId)
                .screenId(screenId)
                .promptLength(promptLength)
                .responseLength(responseLength)
                .usedModel(usedModel)
                .latencyMs(latencyMs)
                .status(status != null ? status : MiraAuditLog.AuditStatus.SUCCESS)
                .errorCode(errorCode)
                .createdAt(LocalDateTime.now())
                .build();
        }
    }
}
