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
}
