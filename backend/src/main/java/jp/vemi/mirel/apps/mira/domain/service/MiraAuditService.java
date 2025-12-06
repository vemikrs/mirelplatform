/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.MiraAuditLogRepository;
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
     * 監査アクション種別.
     */
    public enum AuditAction {
        /** チャット開始 */
        CHAT_START,
        /** チャット応答 */
        CHAT_RESPONSE,
        /** エラー分析 */
        ERROR_ANALYZE,
        /** コンテキストヘルプ */
        CONTEXT_HELP,
        /** Studio エージェント */
        STUDIO_AGENT,
        /** 会話クリア */
        CONVERSATION_CLEAR,
        /** API エラー */
        API_ERROR
    }
    
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
     * @param provider プロバイダー
     * @param latencyMs レイテンシ（ミリ秒）
     * @param promptTokens プロンプトトークン数
     * @param completionTokens 完了トークン数
     * @param status ステータス
     */
    public void logChatResponse(
            String tenantId,
            String userId,
            String conversationId,
            String mode,
            String provider,
            long latencyMs,
            Integer promptTokens,
            Integer completionTokens,
            String status) {
        
        logAsync(AuditLogBuilder.create()
            .action(AuditAction.CHAT_RESPONSE)
            .tenantId(tenantId)
            .userId(userId)
            .conversationId(conversationId)
            .provider(provider)
            .latencyMs(latencyMs)
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .status(status)
            .detail("mode=" + mode));
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
            .action(AuditAction.API_ERROR)
            .tenantId(tenantId)
            .userId(userId)
            .status("ERROR")
            .detail("action=" + action + ", error=" + truncate(errorMessage, 500)));
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
        private AuditAction action;
        private String provider;
        private Long latencyMs;
        private String status;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer promptLength;
        private Integer responseLength;
        private String detail;
        
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
        
        public AuditLogBuilder action(AuditAction action) {
            this.action = action;
            return this;
        }
        
        public AuditLogBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }
        
        public AuditLogBuilder latencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }
        
        public AuditLogBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public AuditLogBuilder promptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }
        
        public AuditLogBuilder completionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
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
        
        public AuditLogBuilder detail(String detail) {
            this.detail = detail;
            return this;
        }
        
        public MiraAuditLog build() {
            return MiraAuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .conversationId(conversationId)
                .action(action != null ? action.name() : null)
                .provider(provider)
                .latencyMs(latencyMs)
                .status(status)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .promptLength(promptLength)
                .responseLength(responseLength)
                .createdAt(LocalDateTime.now())
                .build();
        }
    }
}
