/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer.LayerType;

/**
 * Mira コンテキストレイヤーリポジトリ.
 * <p>
 * 階層コンテキスト（System/Tenant/Organization/User）を管理する。
 * </p>
 */
@Repository
public interface MiraContextLayerRepository extends JpaRepository<MiraContextLayer, String> {

    /**
     * レイヤー種別でコンテキストを検索（優先度順）.
     *
     * @param layerType レイヤー種別
     * @return コンテキストリスト（優先度の高い順）
     */
    List<MiraContextLayer> findByLayerTypeOrderByPriorityDesc(LayerType layerType);

    /**
     * システムレイヤーの有効なコンテキストを取得.
     *
     * @param now 現在日時
     * @return 有効なシステムコンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.layerType = 'SYSTEM' " +
           "AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
           "AND (c.validUntil IS NULL OR c.validUntil > :now) " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveSystemContexts(@Param("now") LocalDateTime now);

    /**
     * テナントレイヤーの有効なコンテキストを取得.
     *
     * @param tenantId テナントID
     * @param now 現在日時
     * @return 有効なテナントコンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.layerType = 'TENANT' " +
           "AND c.tenantId = :tenantId " +
           "AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
           "AND (c.validUntil IS NULL OR c.validUntil > :now) " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveTenantContexts(
            @Param("tenantId") String tenantId,
            @Param("now") LocalDateTime now);

    /**
     * 組織レイヤーの有効なコンテキストを取得.
     *
     * @param tenantId テナントID
     * @param organizationId 組織ID
     * @param now 現在日時
     * @return 有効な組織コンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.layerType = 'ORGANIZATION' " +
           "AND c.tenantId = :tenantId " +
           "AND c.organizationId = :organizationId " +
           "AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
           "AND (c.validUntil IS NULL OR c.validUntil > :now) " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveOrganizationContexts(
            @Param("tenantId") String tenantId,
            @Param("organizationId") String organizationId,
            @Param("now") LocalDateTime now);

    /**
     * ユーザーレイヤーの有効なコンテキストを取得.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param now 現在日時
     * @return 有効なユーザーコンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.layerType = 'USER' " +
           "AND c.tenantId = :tenantId " +
           "AND c.userId = :userId " +
           "AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
           "AND (c.validUntil IS NULL OR c.validUntil > :now) " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveUserContexts(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId,
            @Param("now") LocalDateTime now);

    /**
     * 全階層の有効なコンテキストをマージ取得（System + Tenant + Org + User）.
     * <p>
     * 優先度: User > Organization > Tenant > System
     * </p>
     *
     * @param tenantId テナントID
     * @param organizationId 組織ID（nullable）
     * @param userId ユーザーID
     * @param now 現在日時
     * @return 全階層のコンテキスト（レイヤー優先度 → priority 順）
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE " +
           "((c.layerType = 'SYSTEM') " +
           " OR (c.layerType = 'TENANT' AND c.tenantId = :tenantId) " +
           " OR (c.layerType = 'ORGANIZATION' AND c.tenantId = :tenantId AND c.organizationId = :organizationId) " +
           " OR (c.layerType = 'USER' AND c.tenantId = :tenantId AND c.userId = :userId)) " +
           "AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
           "AND (c.validUntil IS NULL OR c.validUntil > :now) " +
           "ORDER BY c.layerType DESC, c.priority DESC")
    List<MiraContextLayer> findAllActiveContextsForUser(
            @Param("tenantId") String tenantId,
            @Param("organizationId") String organizationId,
            @Param("userId") String userId,
            @Param("now") LocalDateTime now);

    /**
     * 特定キーのコンテキストを取得.
     *
     * @param layerType レイヤー種別
     * @param contextKey コンテキストキー
     * @return コンテキスト
     */
    Optional<MiraContextLayer> findByLayerTypeAndContextKey(LayerType layerType, String contextKey);

    /**
     * テナント・キーでコンテキストを取得.
     *
     * @param layerType レイヤー種別
     * @param tenantId テナントID
     * @param contextKey コンテキストキー
     * @return コンテキスト
     */
    Optional<MiraContextLayer> findByLayerTypeAndTenantIdAndContextKey(
            LayerType layerType, String tenantId, String contextKey);

    /**
     * 期限切れコンテキストを削除.
     *
     * @param now 現在日時
     * @return 削除件数
     */
    @Modifying
    @Query("DELETE FROM MiraContextLayer c WHERE c.validUntil IS NOT NULL AND c.validUntil < :now")
    int deleteExpiredContexts(@Param("now") LocalDateTime now);
}
