/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog.AuditAction;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog.AuditStatus;

/**
 * Mira 監査ログリポジトリ.
 */
@Repository
public interface MiraAuditLogRepository extends JpaRepository<MiraAuditLog, String> {

    /**
     * テナントの監査ログを取得（日時降順）.
     */
    Page<MiraAuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * ユーザの監査ログを取得（日時降順）.
     */
    Page<MiraAuditLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 会話の監査ログを取得.
     */
    List<MiraAuditLog> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * アクション・ステータスで監査ログを検索.
     */
    Page<MiraAuditLog> findByTenantIdAndActionAndStatusOrderByCreatedAtDesc(
            String tenantId, AuditAction action, AuditStatus status, Pageable pageable);

    /**
     * 期間内の監査ログ件数を取得.
     */
    @Query("SELECT COUNT(a) FROM MiraAuditLog a WHERE a.tenantId = :tenantId AND a.createdAt BETWEEN :from AND :to")
    long countByTenantIdAndCreatedAtBetween(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 指定日時より古い監査ログを削除.
     */
    @Modifying
    @Query("DELETE FROM MiraAuditLog a WHERE a.createdAt < :before")
    void deleteByCreatedAtBefore(@Param("before") LocalDateTime before);

    /**
     * エラー件数を取得（モニタリング用）.
     */
    @Query("SELECT COUNT(a) FROM MiraAuditLog a WHERE a.tenantId = :tenantId AND a.status = 'ERROR' AND a.createdAt >= :since")
    long countErrorsSince(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    // ========== セキュリティイベント用メソッド ==========

    /**
     * セキュリティイベントを検索（直近N件）.
     *
     * @param tenantId テナントID
     * @param actions セキュリティ関連アクション
     * @param pageable ページング
     * @return セキュリティイベント
     */
    @Query("SELECT a FROM MiraAuditLog a WHERE a.tenantId = :tenantId AND a.action IN :actions ORDER BY a.createdAt DESC")
    Page<MiraAuditLog> findSecurityEventsByTenant(
            @Param("tenantId") String tenantId,
            @Param("actions") List<AuditAction> actions,
            Pageable pageable);

    /**
     * ユーザのセキュリティイベントを検索.
     *
     * @param userId ユーザID
     * @param actions セキュリティ関連アクション
     * @param since 検索開始日時
     * @return セキュリティイベント
     */
    @Query("SELECT a FROM MiraAuditLog a WHERE a.userId = :userId AND a.action IN :actions AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<MiraAuditLog> findSecurityEventsByUser(
            @Param("userId") String userId,
            @Param("actions") List<AuditAction> actions,
            @Param("since") LocalDateTime since);

    /**
     * プロンプトインジェクション検出件数を取得.
     *
     * @param tenantId テナントID
     * @param since 検索開始日時
     * @return 検出件数
     */
    @Query("SELECT COUNT(a) FROM MiraAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.action IN ('PROMPT_INJECTION_DETECTED', 'PROMPT_INJECTION_BLOCKED') " +
           "AND a.createdAt >= :since")
    long countPromptInjectionEvents(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    /**
     * レート制限超過件数を取得.
     *
     * @param userId ユーザID
     * @param since 検索開始日時
     * @return 超過件数
     */
    @Query("SELECT COUNT(a) FROM MiraAuditLog a WHERE a.userId = :userId " +
           "AND a.action IN ('RATE_LIMIT_EXCEEDED', 'QUOTA_EXHAUSTED') " +
           "AND a.createdAt >= :since")
    long countRateLimitEvents(@Param("userId") String userId, @Param("since") LocalDateTime since);

    // ========== モニタリング用メソッド ==========

    /**
     * 平均レイテンシを取得.
     *
     * @param tenantId テナントID
     * @param since 検索開始日時
     * @return 平均レイテンシ（ms）
     */
    @Query("SELECT AVG(a.latencyMs) FROM MiraAuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.latencyMs IS NOT NULL AND a.createdAt >= :since")
    Double getAverageLatency(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    /**
     * 成功率を取得.
     *
     * @param tenantId テナントID
     * @param since 検索開始日時
     * @return 成功率（0.0〜1.0）
     */
    @Query("SELECT CAST(SUM(CASE WHEN a.status = 'SUCCESS' THEN 1 ELSE 0 END) AS double) / " +
           "CAST(COUNT(a) AS double) FROM MiraAuditLog a " +
           "WHERE a.tenantId = :tenantId AND a.createdAt >= :since")
    Double getSuccessRate(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    /**
     * モード別使用統計を取得.
     *
     * @param tenantId テナントID
     * @param since 検索開始日時
     * @return モード別件数（mode, count）
     */
    @Query("SELECT a.mode, COUNT(a) FROM MiraAuditLog a " +
           "WHERE a.tenantId = :tenantId AND a.mode IS NOT NULL AND a.createdAt >= :since " +
           "GROUP BY a.mode ORDER BY COUNT(a) DESC")
    List<Object[]> getModeUsageStatistics(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    /**
     * 合計トークン使用量を取得.
     *
     * @param tenantId テナントID
     * @param since 検索開始日時
     * @return 合計トークン数
     */
    @Query("SELECT COALESCE(SUM(a.totalTokens), 0) FROM MiraAuditLog a " +
           "WHERE a.tenantId = :tenantId AND a.createdAt >= :since")
    long getTotalTokenUsage(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);
}
