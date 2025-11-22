/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.OtpAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OtpAuditLogリポジトリ.
 */
@Repository
public interface OtpAuditLogRepository extends JpaRepository<OtpAuditLog, UUID> {
    
    /**
     * ユーザー別ログ取得
     * 
     * @param systemUserId SystemUser ID
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 監査ログリスト
     */
    List<OtpAuditLog> findBySystemUserIdAndCreatedAtBetween(
        UUID systemUserId, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    /**
     * メールアドレス別ログ取得
     * 
     * @param email メールアドレス
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return 監査ログリスト
     */
    List<OtpAuditLog> findByEmailAndCreatedAtBetween(
        String email, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
    
    /**
     * 古いログを削除
     * 
     * @param cutoffDate 削除基準日時
     * @return 削除件数
     */
    @Modifying
    int deleteByCreatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * ユーザー削除時に監査ログを匿名化
     * 
     * @param systemUserId SystemUser ID
     */
    @Modifying
    @Query("UPDATE OtpAuditLog o SET o.systemUserId = null, o.email = 'anonymized@deleted.user' WHERE o.systemUserId = :systemUserId")
    void anonymizeBySystemUserId(@Param("systemUserId") UUID systemUserId);
}
