/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.organization.model.Organization;
import jp.vemi.mirel.foundation.organization.model.OrganizationType;

/**
 * 組織リポジトリ.
 */
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    /**
     * テナントIDで組織一覧を取得.
     */
    List<Organization> findByTenantId(String tenantId);

    /**
     * 親IDで子組織一覧を取得.
     */
    List<Organization> findByParentId(String parentId);

    /**
     * テナントIDと種別で組織一覧を取得.
     */
    List<Organization> findByTenantIdAndType(String tenantId, OrganizationType type);

    /**
     * テナントIDとコードで組織を取得.
     */
    Optional<Organization> findByTenantIdAndCode(String tenantId, String code);

    /**
     * テナントIDで有効な組織一覧を取得.
     */
    List<Organization> findByTenantIdAndIsActiveTrue(String tenantId);

    /**
     * パスの前方一致で子孫を取得.
     */
    List<Organization> findByPathStartingWith(String pathPrefix);

    /**
     * テナントIDと親IDがnull（ルート組織）を取得.
     */
    List<Organization> findByTenantIdAndParentIdIsNull(String tenantId);
}
