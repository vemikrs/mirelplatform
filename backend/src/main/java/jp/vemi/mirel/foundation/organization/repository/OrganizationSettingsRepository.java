/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.organization.model.OrganizationSettings;

/**
 * 組織設定リポジトリ.
 */
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, String> {

    /**
     * 組織IDと期間コードで設定を取得.
     */
    Optional<OrganizationSettings> findByOrganizationIdAndPeriodCode(String organizationId, String periodCode);

    /**
     * 組織IDで設定一覧を取得.
     */
    List<OrganizationSettings> findByOrganizationId(String organizationId);

    /**
     * 組織IDで最新の設定（期間コードがnullまたは最新）を取得.
     */
    Optional<OrganizationSettings> findFirstByOrganizationIdAndPeriodCodeIsNullOrderByCreateDateDesc(String organizationId);
}
