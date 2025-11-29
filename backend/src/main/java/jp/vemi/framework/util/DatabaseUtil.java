/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.framework.util;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jp.vemi.mirel.foundation.abst.dao.entity.TenantSystemMaster;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantSystemMasterRepository;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.entity.UserTenant;
import jp.vemi.mirel.foundation.abst.dao.repository.ApplicationLicenseRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.FeatureFlagRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserTenantRepository;

@Component("databaseUtil")
@Order(10)
public class DatabaseUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    private static TenantSystemMasterRepository getTenantSystemMasterRepository() {
        return context.getBean(TenantSystemMasterRepository.class);
    }

    private static SystemUserRepository getSystemUserRepository() {
        return context.getBean(SystemUserRepository.class);
    }

    private static UserRepository getUserRepository() {
        return context.getBean(UserRepository.class);
    }

    private static TenantRepository getTenantRepository() {
        return context.getBean(TenantRepository.class);
    }

    private static UserTenantRepository getUserTenantRepository() {
        return context.getBean(UserTenantRepository.class);
    }

    private static ApplicationLicenseRepository getApplicationLicenseRepository() {
        return context.getBean(ApplicationLicenseRepository.class);
    }

    private static FeatureFlagRepository getFeatureFlagRepository() {
        return context.getBean(FeatureFlagRepository.class);
    }

    private static PasswordEncoder getPasswordEncoder() {
        return context.getBean(PasswordEncoder.class);
    }

    public static void initializeDefaultTenant() {
        String defaultTenantId = "default";
        String jwkSetUriKey = "jwkSetUri";
        String defaultJwkSetUri = "https://auth.vemi.jp/jwk"; // デフォルトのJWK Set URI

        TenantSystemMasterRepository tenantSystemMasterRepository = getTenantSystemMasterRepository();
        Optional<TenantSystemMaster> tenant = tenantSystemMasterRepository.findByTenantIdAndKey(defaultTenantId,
                jwkSetUriKey);
        if (!tenant.isPresent()) {
            TenantSystemMaster newTenant = new TenantSystemMaster();
            newTenant.setTenantId(defaultTenantId);
            newTenant.setKey(jwkSetUriKey);
            newTenant.setValue(defaultJwkSetUri);
            tenantSystemMasterRepository.save(newTenant);
        }
    }

    /**
     * SaaS認証テストデータを初期化 (開発環境のみ)
     * - SystemUser: CSVから読み込み (admin, user01-user10)
     * - Tenant: default, enterprise-001
     * - User: 各SystemUserに紐づくアプリケーションユーザー
     * - UserTenant: テナント参加情報
     * - ApplicationLicense: FREE/PRO/MAX ライセンス
     */
    public static void initializeSaasTestData() {
        SystemUserRepository systemUserRepo = getSystemUserRepository();
        UserRepository userRepo = getUserRepository();
        TenantRepository tenantRepo = getTenantRepository();
        UserTenantRepository userTenantRepo = getUserTenantRepository();
        ApplicationLicenseRepository licenseRepo = getApplicationLicenseRepository();
        PasswordEncoder passwordEncoder = getPasswordEncoder();

        Instant now = Instant.now();

        // 1. SystemUsers (CSVから読み込み)
        List<SystemUser> systemUsers = CsvTestDataLoader.loadSystemUsers(passwordEncoder);
        for (SystemUser systemUser : systemUsers) {
            if (!systemUserRepo.existsById(systemUser.getId())) {
                systemUserRepo.save(systemUser);
            }
        }

        // 2. Tenants
        if (!tenantRepo.existsById("default")) {
            Tenant defaultTenant = new Tenant();
            defaultTenant.setTenantId("default");
            defaultTenant.setTenantName("default");
            defaultTenant.setDisplayName("Default Workspace");
            defaultTenant.setIsActive(true);
            tenantRepo.save(defaultTenant);
        }

        if (!tenantRepo.existsById("enterprise-001")) {
            Tenant enterpriseTenant = new Tenant();
            enterpriseTenant.setTenantId("enterprise-001");
            enterpriseTenant.setTenantName("enterprise-001");
            enterpriseTenant.setDisplayName("Enterprise Workspace");
            enterpriseTenant.setIsActive(true);
            tenantRepo.save(enterpriseTenant);
        }

        // 3. Users (Application Data - CSVから読み込み)
        List<User> users = CsvTestDataLoader.loadUsers();
        for (User user : users) {
            User existingUser = userRepo.findById(user.getUserId()).orElse(null);
            if (existingUser == null) {
                userRepo.save(user);
            } else {
                // 既存ユーザーの場合、nullなフィールドを更新
                boolean needUpdate = false;
                if (existingUser.getUsername() == null) {
                    existingUser.setUsername(user.getUsername());
                    needUpdate = true;
                }
                if (existingUser.getEmail() == null) {
                    existingUser.setEmail(user.getEmail());
                    needUpdate = true;
                }
                // rolesがnullの場合はCSVから読み込んだ値で更新
                if (existingUser.getRoles() == null && user.getRoles() != null) {
                    existingUser.setRoles(user.getRoles());
                    needUpdate = true;
                }
                if (needUpdate) {
                    userRepo.save(existingUser);
                }
            }
        }

        // 4. UserTenant (Associations)
        if (!userTenantRepo.findByUserIdAndTenantId("user-admin-001", "default").isPresent()) {
            UserTenant utAdminDefault = new UserTenant();
            utAdminDefault.setId("ut-admin-default");
            utAdminDefault.setUserId("user-admin-001");
            utAdminDefault.setTenantId("default");
            utAdminDefault.setRoleInTenant("ADMIN");
            utAdminDefault.setIsDefault(true);
            utAdminDefault.setJoinedAt(now);
            userTenantRepo.save(utAdminDefault);
        }

        // user01-user10をdefaultテナントに追加
        for (int i = 1; i <= 10; i++) {
            String userId = String.format("user-regular-%03d", i);
            String utId = String.format("ut-user-default-%03d", i);
            if (!userTenantRepo.findByUserIdAndTenantId(userId, "default").isPresent()) {
                UserTenant ut = new UserTenant();
                ut.setId(utId);
                ut.setUserId(userId);
                ut.setTenantId("default");
                ut.setRoleInTenant("MEMBER");
                ut.setIsDefault(true);
                ut.setJoinedAt(now);
                userTenantRepo.save(ut);
            }
        }

        if (!userTenantRepo.findByUserIdAndTenantId("user-admin-001", "enterprise-001").isPresent()) {
            UserTenant utAdminEnterprise = new UserTenant();
            utAdminEnterprise.setId("ut-admin-enterprise");
            utAdminEnterprise.setUserId("user-admin-001");
            utAdminEnterprise.setTenantId("enterprise-001");
            utAdminEnterprise.setRoleInTenant("ADMIN");
            utAdminEnterprise.setIsDefault(false);
            utAdminEnterprise.setJoinedAt(now);
            userTenantRepo.save(utAdminEnterprise);
        }

        // 5. ApplicationLicenses
        if (!licenseRepo.existsById("license-user-free")) {
            ApplicationLicense userFreeLicense = new ApplicationLicense();
            userFreeLicense.setId("license-user-free");
            userFreeLicense.setSubjectType(ApplicationLicense.SubjectType.USER);
            userFreeLicense.setSubjectId("user-regular-001");
            userFreeLicense.setApplicationId("promarker");
            userFreeLicense.setTier(ApplicationLicense.LicenseTier.FREE);
            userFreeLicense.setGrantedAt(now);
            userFreeLicense.setExpiresAt(null);
            licenseRepo.save(userFreeLicense);
        }

        if (!licenseRepo.existsById("license-tenant-max")) {
            ApplicationLicense tenantMaxLicense = new ApplicationLicense();
            tenantMaxLicense.setId("license-tenant-max");
            tenantMaxLicense.setSubjectType(ApplicationLicense.SubjectType.TENANT);
            tenantMaxLicense.setSubjectId("default");
            tenantMaxLicense.setApplicationId("promarker");
            tenantMaxLicense.setTier(ApplicationLicense.LicenseTier.MAX);
            tenantMaxLicense.setGrantedAt(now);
            tenantMaxLicense.setExpiresAt(now.plusSeconds(365 * 24 * 3600)); // 1 year
            licenseRepo.save(tenantMaxLicense);
        }

        if (!licenseRepo.existsById("license-tenant-pro")) {
            ApplicationLicense tenantProLicense = new ApplicationLicense();
            tenantProLicense.setId("license-tenant-pro");
            tenantProLicense.setSubjectType(ApplicationLicense.SubjectType.TENANT);
            tenantProLicense.setSubjectId("enterprise-001");
            tenantProLicense.setApplicationId("promarker");
            tenantProLicense.setTier(ApplicationLicense.LicenseTier.PRO);
            tenantProLicense.setGrantedAt(now);
            tenantProLicense.setExpiresAt(now.plusSeconds(90 * 24 * 3600)); // 3 months
            licenseRepo.save(tenantProLicense);
        }

        // 6. FeatureFlags
        initializeFeatureFlagData();
    }

    /**
     * フィーチャーフラグ初期データを投入 (CSVから読み込み)
     */
    public static void initializeFeatureFlagData() {
        FeatureFlagRepository featureFlagRepo = getFeatureFlagRepository();

        List<FeatureFlag> featureFlags = CsvTestDataLoader.loadFeatureFlags();
        for (FeatureFlag flag : featureFlags) {
            if (!featureFlagRepo.existsById(flag.getId())) {
                featureFlagRepo.save(flag);
            }
        }
    }

}
