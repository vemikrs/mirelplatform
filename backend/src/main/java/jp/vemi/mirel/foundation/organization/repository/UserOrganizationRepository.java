/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;

/**
 * ユーザー所属リポジトリ.
 */
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, String> {

    /**
     * ユーザーIDで所属一覧を取得.
     */
    List<UserOrganization> findByUserId(String userId);

    /**
     * 組織IDで所属一覧を取得.
     */
    List<UserOrganization> findByOrganizationId(String organizationId);

    /**
     * 組織ID一覧で所属一覧を取得.
     */
    List<UserOrganization> findByOrganizationIdIn(List<String> organizationIds);

    /**
     * ユーザーIDと組織IDで所属を取得.
     */
    List<UserOrganization> findByUserIdAndOrganizationId(String userId, String organizationId);

    /**
     * ユーザーIDとロールで所属一覧を取得.
     */
    List<UserOrganization> findByUserIdAndRole(String userId, String role);
}
