/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog.AuditAction;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog.AuditStatus;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraAuditLogRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * セキュリティ監査ロガー.
 * <p>
 * Mira のセキュリティイベントを監査ログに記録する。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuditLogger {

    private static final Logger securityLog = LoggerFactory.getLogger("mira.security");

    private final MiraAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * セキュリティイベントを記録.
     *
     * @param event セキュリティイベント
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(SecurityEvent event) {
        // ファイルログに即座に記録
        String jsonDetails = toJson(event.getDetails());
        securityLog.warn("[SECURITY] event={}, tenant={}, user={}, severity={}, details={}",
                event.getAction(),
                event.getTenantId(),
                event.getUserId(),
                event.getSeverity(),
                jsonDetails);

        // データベースに永続化
        try {
            MiraAuditLog auditLog = MiraAuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(event.getTenantId())
                    .userId(event.getUserId())
                    .conversationId(event.getConversationId())
                    .action(event.getAction())
                    .mode(event.getMode())
                    .status(AuditStatus.SUCCESS)
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

            if (log.isDebugEnabled()) {
                log.debug("[SecurityAuditLogger] Logged security event: action={}, id={}",
                        event.getAction(), auditLog.getId());
            }
        } catch (Exception e) {
            log.error("[SecurityAuditLogger] Failed to persist security event: action={}",
                    event.getAction(), e);
        }
    }

    /**
     * プロンプトインジェクション検出イベントを記録.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param conversationId 会話ID
     * @param score 検出スコア
     * @param patterns 検出パターン
     * @param blocked ブロックされたか
     */
    public void logPromptInjectionEvent(
            String tenantId,
            String userId,
            String conversationId,
            int score,
            List<String> patterns,
            boolean blocked) {

        AuditAction action = blocked
                ? AuditAction.PROMPT_INJECTION_BLOCKED
                : AuditAction.PROMPT_INJECTION_DETECTED;

        SecurityEvent event = SecurityEvent.builder()
                .action(action)
                .severity(blocked ? Severity.HIGH : Severity.MEDIUM)
                .tenantId(tenantId)
                .userId(userId)
                .conversationId(conversationId)
                .details(Map.of(
                        "score", score,
                        "patterns", patterns,
                        "blocked", blocked))
                .build();

        logSecurityEvent(event);
    }

    /**
     * PII 検出イベントを記録.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param conversationId 会話ID
     * @param piiTypes 検出された PII タイプ
     */
    public void logPiiDetectedEvent(
            String tenantId,
            String userId,
            String conversationId,
            List<String> piiTypes) {

        SecurityEvent event = SecurityEvent.builder()
                .action(AuditAction.PII_DETECTED)
                .severity(Severity.MEDIUM)
                .tenantId(tenantId)
                .userId(userId)
                .conversationId(conversationId)
                .details(Map.of("piiTypes", piiTypes))
                .build();

        logSecurityEvent(event);
    }

    /**
     * レート制限超過イベントを記録.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param limit 制限値
     * @param current 現在値
     */
    public void logRateLimitExceeded(
            String tenantId,
            String userId,
            int limit,
            int current) {

        SecurityEvent event = SecurityEvent.builder()
                .action(AuditAction.RATE_LIMIT_EXCEEDED)
                .severity(Severity.LOW)
                .tenantId(tenantId)
                .userId(userId)
                .details(Map.of(
                        "limit", limit,
                        "current", current))
                .build();

        logSecurityEvent(event);
    }

    /**
     * 認可拒否イベントを記録.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param resource アクセス対象リソース
     * @param reason 拒否理由
     */
    public void logAuthorizationDenied(
            String tenantId,
            String userId,
            String resource,
            String reason) {

        SecurityEvent event = SecurityEvent.builder()
                .action(AuditAction.AUTHORIZATION_DENIED)
                .severity(Severity.MEDIUM)
                .tenantId(tenantId)
                .userId(userId)
                .details(Map.of(
                        "resource", resource,
                        "reason", reason))
                .build();

        logSecurityEvent(event);
    }

    /**
     * 出力フィルタリングイベントを記録.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param conversationId 会話ID
     * @param filteredPatterns フィルタされたパターン
     */
    public void logOutputFiltered(
            String tenantId,
            String userId,
            String conversationId,
            List<String> filteredPatterns) {

        SecurityEvent event = SecurityEvent.builder()
                .action(AuditAction.OUTPUT_FILTERED)
                .severity(Severity.LOW)
                .tenantId(tenantId)
                .userId(userId)
                .conversationId(conversationId)
                .details(Map.of("filteredPatterns", filteredPatterns))
                .build();

        logSecurityEvent(event);
    }

    private String toJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("[SecurityAuditLogger] Failed to serialize details to JSON", e);
            return "{}";
        }
    }

    /**
     * セキュリティイベント.
     */
    @Data
    @Builder
    public static class SecurityEvent {
        private AuditAction action;
        private Severity severity;
        private String tenantId;
        private String userId;
        private String conversationId;
        private String mode;
        private String ipAddress;
        private String userAgent;
        private Map<String, Object> details;
    }

    /**
     * 重大度.
     */
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
