/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextSnapshot;

/**
 * Mira コンテキストスナップショットリポジトリ.
 */
@Repository
public interface MiraContextSnapshotRepository extends JpaRepository<MiraContextSnapshot, String> {

    /**
     * テナント・ユーザで直近のスナップショットを取得.
     */
    List<MiraContextSnapshot> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, String userId);

    /**
     * アプリ・画面でスナップショットを検索.
     */
    List<MiraContextSnapshot> findByAppIdAndScreenIdOrderByCreatedAtDesc(String appId, String screenId);

    /**
     * 指定日時より古いスナップショットを削除.
     */
    @Modifying
    @Query("DELETE FROM MiraContextSnapshot c WHERE c.createdAt < :before")
    void deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
