/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraIndexingProgress;

/**
 * Mira インデックス処理進捗リポジトリ.
 */
@Repository
public interface MiraIndexingProgressRepository extends JpaRepository<MiraIndexingProgress, String> {

    /**
     * ファイルIDで進捗を検索.
     */
    Optional<MiraIndexingProgress> findByFileId(String fileId);
}
