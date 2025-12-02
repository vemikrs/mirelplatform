/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.FeatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * フィーチャーフラグリポジトリ.
 */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {

    /**
     * featureKeyで検索
     */
    Optional<FeatureFlag> findByFeatureKeyAndDeleteFlagFalse(String featureKey);

    /**
     * featureKeyの存在チェック
     */
    boolean existsByFeatureKeyAndDeleteFlagFalse(String featureKey);

    /**
     * アプリケーションIDで検索
     */
    List<FeatureFlag> findByApplicationIdAndDeleteFlagFalse(String applicationId);

    /**
     * 開発中の機能を検索
     */
    List<FeatureFlag> findByInDevelopmentTrueAndDeleteFlagFalse();

    /**
     * ステータスで検索
     */
    List<FeatureFlag> findByStatusAndDeleteFlagFalse(FeatureStatus status);

    /**
     * 全件取得（削除されていないもの）
     */
    List<FeatureFlag> findByDeleteFlagFalse();

    /**
     * ページング対応検索（削除されていないもの）
     */
    Page<FeatureFlag> findByDeleteFlagFalse(Pageable pageable);

    /**
     * アプリケーションIDでページング検索
     */
    Page<FeatureFlag> findByApplicationIdAndDeleteFlagFalse(String applicationId, Pageable pageable);

    /**
     * 複合条件検索
     */
    @Query("SELECT ff FROM FeatureFlag ff WHERE " +
           "ff.deleteFlag = false AND " +
           "(:applicationId IS NULL OR ff.applicationId = :applicationId) AND " +
           "(:status IS NULL OR ff.status = :status) AND " +
           "(:inDevelopment IS NULL OR ff.inDevelopment = :inDevelopment) AND " +
           "(:searchTerm IS NULL OR " +
           "  LOWER(ff.featureKey) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "  LOWER(ff.featureName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "  LOWER(ff.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<FeatureFlag> findWithFilters(
        @Param("applicationId") String applicationId,
        @Param("status") FeatureStatus status,
        @Param("inDevelopment") Boolean inDevelopment,
        @Param("searchTerm") String searchTerm,
        Pageable pageable);

    /**
     * 有効なフィーチャーフラグを取得（ユーザー・テナント条件）
     * Phase 1では enabledByDefault = true または enabledForUserIds/enabledForTenantIds に含まれるものを返す
     */
    @Query("SELECT ff FROM FeatureFlag ff WHERE " +
           "ff.deleteFlag = false AND " +
           "(ff.enabledByDefault = true OR " +
           " (ff.enabledForUserIds IS NOT NULL AND ff.enabledForUserIds LIKE CONCAT('%', :userId, '%')) OR " +
           " (ff.enabledForTenantIds IS NOT NULL AND ff.enabledForTenantIds LIKE CONCAT('%', :tenantId, '%')))")
    List<FeatureFlag> findEffectiveFeatures(
        @Param("userId") String userId,
        @Param("tenantId") String tenantId);

    /**
     * アプリケーションごとの有効なフィーチャーフラグを取得
     */
    @Query("SELECT ff FROM FeatureFlag ff WHERE " +
           "ff.deleteFlag = false AND " +
           "ff.applicationId = :applicationId AND " +
           "(ff.enabledByDefault = true OR " +
           " (ff.enabledForUserIds IS NOT NULL AND ff.enabledForUserIds LIKE CONCAT('%', :userId, '%')) OR " +
           " (ff.enabledForTenantIds IS NOT NULL AND ff.enabledForTenantIds LIKE CONCAT('%', :tenantId, '%')))")
    List<FeatureFlag> findEffectiveFeaturesForApplication(
        @Param("applicationId") String applicationId,
        @Param("userId") String userId,
        @Param("tenantId") String tenantId);
}
