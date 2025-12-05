/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.UserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserTenantリポジトリ.
 */
@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, String> {

    /**
     * ユーザIDに紐づく全テナント関連を取得
     */
    @Query("SELECT ut FROM UserTenant ut WHERE ut.userId = :userId AND ut.deleteFlag = false AND ut.leftAt IS NULL")
    List<UserTenant> findByUserId(@Param("userId") String userId);

    /**
     * テナントIDに紐づく全ユーザ関連を取得
     */
    @Query("SELECT ut FROM UserTenant ut WHERE ut.tenantId = :tenantId AND ut.deleteFlag = false AND ut.leftAt IS NULL")
    List<UserTenant> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * ユーザのデフォルトテナント関連を取得
     */
    @Query("SELECT ut FROM UserTenant ut WHERE ut.userId = :userId AND ut.isDefault = true AND ut.deleteFlag = false AND ut.leftAt IS NULL")
    Optional<UserTenant> findDefaultByUserId(@Param("userId") String userId);

    /**
     * ユーザとテナントの関連を取得
     */
    @Query("SELECT ut FROM UserTenant ut WHERE ut.userId = :userId AND ut.tenantId = :tenantId AND ut.deleteFlag = false")
    Optional<UserTenant> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);

    /**
     * テナントIDに紐づく有効なユーザ数をカウント
     */
    @Query("SELECT COUNT(ut) FROM UserTenant ut WHERE ut.tenantId = :tenantId AND ut.deleteFlag = false AND ut.leftAt IS NULL")
    Integer countByTenantId(@Param("tenantId") String tenantId);
}
