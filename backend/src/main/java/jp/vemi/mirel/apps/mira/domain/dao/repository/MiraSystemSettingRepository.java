/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraSystemSetting;

/**
 * Mira システム設定リポジトリ.
 */
@Repository
public interface MiraSystemSettingRepository extends JpaRepository<MiraSystemSetting, String> {
}
