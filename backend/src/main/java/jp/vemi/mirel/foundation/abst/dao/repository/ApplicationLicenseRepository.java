/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ApplicationLicenseリポジトリ.
 */
@Repository
public interface ApplicationLicenseRepository extends JpaRepository<ApplicationLicense, String> {

    /**
     * 有効なライセンスを取得（USER/TENANTスコープ両方）
     */
    @Query("SELECT al FROM ApplicationLicense al WHERE " +
           "((al.subjectType = 'USER' AND al.subjectId = :userId) OR " +
           " (al.subjectType = 'TENANT' AND al.subjectId = :tenantId)) AND " +
           "al.deleteFlag = false AND " +
           "(al.expiresAt IS NULL OR al.expiresAt > :now)")
    List<ApplicationLicense> findEffectiveLicenses(
        @Param("userId") String userId, 
        @Param("tenantId") String tenantId,
        @Param("now") Instant now);

    /**
     * ユーザの有効なライセンスを取得
     */
    @Query("SELECT al FROM ApplicationLicense al WHERE " +
           "al.subjectType = 'USER' AND al.subjectId = :userId AND " +
           "al.deleteFlag = false AND " +
           "(al.expiresAt IS NULL OR al.expiresAt > :now)")
    List<ApplicationLicense> findEffectiveUserLicenses(
        @Param("userId") String userId,
        @Param("now") Instant now);

    /**
     * テナントの有効なライセンスを取得
     */
    @Query("SELECT al FROM ApplicationLicense al WHERE " +
           "al.subjectType = 'TENANT' AND al.subjectId = :tenantId AND " +
           "al.deleteFlag = false AND " +
           "(al.expiresAt IS NULL OR al.expiresAt > :now)")
    List<ApplicationLicense> findEffectiveTenantLicenses(
        @Param("tenantId") String tenantId,
        @Param("now") Instant now);

    /**
     * 特定のアプリケーションのライセンスを取得
     */
    @Query("SELECT al FROM ApplicationLicense al WHERE " +
           "al.subjectType = :subjectType AND al.subjectId = :subjectId AND " +
           "al.applicationId = :applicationId AND al.deleteFlag = false")
    Optional<ApplicationLicense> findBySubjectAndApplication(
        @Param("subjectType") SubjectType subjectType,
        @Param("subjectId") String subjectId,
        @Param("applicationId") String applicationId);
}
