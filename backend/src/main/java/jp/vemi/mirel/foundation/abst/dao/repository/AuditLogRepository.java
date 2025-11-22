/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * AuditLogリポジトリ.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /**
     * ユーザIDで検索（ページング）
     */
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.deleteFlag = false ORDER BY al.createdAt DESC")
    Page<AuditLog> findByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * テナントIDで検索（ページング）
     */
    @Query("SELECT al FROM AuditLog al WHERE al.tenantId = :tenantId AND al.deleteFlag = false ORDER BY al.createdAt DESC")
    Page<AuditLog> findByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * イベントタイプで検索
     */
    @Query("SELECT al FROM AuditLog al WHERE al.eventType = :eventType AND " +
           "al.createdAt BETWEEN :startDate AND :endDate AND al.deleteFlag = false ORDER BY al.createdAt DESC")
    List<AuditLog> findByEventTypeAndDateRange(
        @Param("eventType") String eventType,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate);
}
