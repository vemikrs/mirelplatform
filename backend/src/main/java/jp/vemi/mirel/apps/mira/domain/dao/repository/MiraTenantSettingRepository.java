/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraTenantSetting;

/**
 * Mira テナント設定リポジトリ.
 */
@Repository
public interface MiraTenantSettingRepository extends JpaRepository<MiraTenantSetting, MiraTenantSetting.PK> {
    List<MiraTenantSetting> findByTenantId(String tenantId);
}
