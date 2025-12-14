/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraModelRegistry;

/**
 * Mira モデルレジストリリポジトリ.
 * <p>
 * AIモデル情報の取得・更新を行います。
 * </p>
 */
@Repository
public interface MiraModelRegistryRepository extends JpaRepository<MiraModelRegistry, String> {

    /**
     * プロバイダごとの有効なモデル一覧を取得.
     * 
     * @param provider
     *            プロバイダ名
     * @return 有効なモデルリスト
     */
    List<MiraModelRegistry> findByProviderAndIsActiveTrue(String provider);

    /**
     * すべての有効なモデル一覧を取得.
     * 
     * @return 有効なモデルリスト
     */
    List<MiraModelRegistry> findByIsActiveTrue();

    /**
     * プロバイダとモデル名でモデルを検索.
     * 
     * @param provider
     *            プロバイダ名
     * @param modelName
     *            モデル名
     * @return モデル情報（Optional）
     */
    Optional<MiraModelRegistry> findByProviderAndModelName(String provider, String modelName);
}
