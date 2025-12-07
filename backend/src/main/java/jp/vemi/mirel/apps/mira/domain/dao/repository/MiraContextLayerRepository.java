/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer.ContextScope;

/**
 * Mira コンテキストレイヤーリポジトリ.
 * <p>
 * 階層コンテキスト（System/Tenant/Organization/User）を管理する。
 * </p>
 */
@Repository
public interface MiraContextLayerRepository extends JpaRepository<MiraContextLayer, String> {

    /**
     * スコープでコンテキストを検索（優先度順）.
     *
     * @param scope スコープ
     * @return コンテキストリスト（優先度の高い順）
     */
    List<MiraContextLayer> findByScopeAndEnabledTrueOrderByPriorityDesc(ContextScope scope);

    /**
     * システムスコープの有効なコンテキストを取得.
     *
     * @return 有効なシステムコンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.scope = 'SYSTEM' " +
           "AND c.enabled = true " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveSystemContexts();

    /**
     * テナントスコープの有効なコンテキストを取得.
     *
     * @param scopeId テナントID（scopeId に格納）
     * @return 有効なテナントコンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.scope = 'TENANT' " +
           "AND c.scopeId = :scopeId " +
           "AND c.enabled = true " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveTenantContexts(@Param("scopeId") String scopeId);

    /**
     * 組織スコープの有効なコンテキストを取得.
     *
     * @param scopeId 組織ID（scopeId に格納）
     * @return 有効な組織コンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.scope = 'ORGANIZATION' " +
           "AND c.scopeId = :scopeId " +
           "AND c.enabled = true " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveOrganizationContexts(@Param("scopeId") String scopeId);

    /**
     * ユーザースコープの有効なコンテキストを取得.
     *
     * @param scopeId ユーザーID（scopeId に格納）
     * @return 有効なユーザーコンテキスト
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE c.scope = 'USER' " +
           "AND c.scopeId = :scopeId " +
           "AND c.enabled = true " +
           "ORDER BY c.priority DESC")
    List<MiraContextLayer> findActiveUserContexts(@Param("scopeId") String scopeId);

    /**
     * 全階層の有効なコンテキストをマージ取得（System + Tenant + Org + User）.
     * <p>
     * 優先度: User > Organization > Tenant > System
     * </p>
     *
     * @param tenantId テナントID
     * @param organizationId 組織ID（nullable）
     * @param userId ユーザーID
     * @return 全階層のコンテキスト（スコープ優先度 → priority 順）
     */
    @Query("SELECT c FROM MiraContextLayer c WHERE " +
           "((c.scope = 'SYSTEM') " +
           " OR (c.scope = 'TENANT' AND c.scopeId = :tenantId) " +
           " OR (c.scope = 'ORGANIZATION' AND c.scopeId = :organizationId) " +
           " OR (c.scope = 'USER' AND c.scopeId = :userId)) " +
           "AND c.enabled = true " +
           "ORDER BY c.scope DESC, c.priority DESC")
    List<MiraContextLayer> findAllActiveContextsForUser(
            @Param("tenantId") String tenantId,
            @Param("organizationId") String organizationId,
            @Param("userId") String userId);

    /**
     * スコープとカテゴリでコンテキストを取得.
     *
     * @param scope スコープ
     * @param category カテゴリ
     * @return コンテキスト
     */
    Optional<MiraContextLayer> findByScopeAndCategory(ContextScope scope, String category);

    /**
     * スコープ、スコープID、カテゴリでコンテキストを取得.
     *
     * @param scope スコープ
     * @param scopeId スコープID
     * @param category カテゴリ
     * @return コンテキスト
     */
    Optional<MiraContextLayer> findByScopeAndScopeIdAndCategory(
            ContextScope scope, String scopeId, String category);

    /**
     * カテゴリでコンテキストを検索.
     *
     * @param category カテゴリ
     * @return コンテキストリスト
     */
    List<MiraContextLayer> findByCategoryAndEnabledTrueOrderByPriorityDesc(String category);
}
