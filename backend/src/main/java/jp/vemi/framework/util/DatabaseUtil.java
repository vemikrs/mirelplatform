/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.framework.util;

import java.time.Instant;
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
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.entity.UserTenant;
import jp.vemi.mirel.foundation.abst.dao.repository.ApplicationLicenseRepository;
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
     * - SystemUser: admin@example.com / user@example.com (password: password123)
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

        // 既にデータがあればスキップ -> 既存データがある場合も補正のためにチェックを行うように変更
        // if (systemUserRepo.count() > 0) {
        // return;
        // }

        Instant now = Instant.now();
        String encodedPassword = passwordEncoder.encode("password123");

        // 1. SystemUsers
        UUID adminSystemUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        if (!systemUserRepo.existsById(adminSystemUserId)) {
            SystemUser adminSystemUser = new SystemUser();
            adminSystemUser.setId(adminSystemUserId);
            adminSystemUser.setUsername("admin");
            adminSystemUser.setEmail("admin@example.com");
            adminSystemUser.setPasswordHash(encodedPassword);
            adminSystemUser.setEmailVerified(true);
            adminSystemUser.setIsActive(true);
            adminSystemUser.setAccountLocked(false);
            adminSystemUser.setFailedLoginAttempts(0);
            systemUserRepo.save(adminSystemUser);
        }

        UUID userSystemUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        if (!systemUserRepo.existsById(userSystemUserId)) {
            SystemUser userSystemUser = new SystemUser();
            userSystemUser.setId(userSystemUserId);
            userSystemUser.setUsername("user");
            userSystemUser.setEmail("user@example.com");
            userSystemUser.setPasswordHash(encodedPassword);
            userSystemUser.setEmailVerified(true);
            userSystemUser.setIsActive(true);
            userSystemUser.setAccountLocked(false);
            userSystemUser.setFailedLoginAttempts(0);
            systemUserRepo.save(userSystemUser);
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

        // 3. Users (Application Data)
        // Admin User
        User adminUser = userRepo.findById("user-admin-001").orElse(new User());
        boolean needSaveAdmin = false;
        if (adminUser.getUserId() == null) {
            adminUser.setUserId("user-admin-001");
            adminUser.setSystemUserId(adminSystemUserId);
            adminUser.setTenantId("default");
            adminUser.setDisplayName("Admin User");
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setIsActive(true);
            adminUser.setEmailVerified(true);
            adminUser.setLastLoginAt(now);
            needSaveAdmin = true;
        }
        // Ensure username/email are set (fix for null username issue)
        if (adminUser.getUsername() == null) {
            adminUser.setUsername("admin");
            needSaveAdmin = true;
        }
        if (adminUser.getEmail() == null) {
            adminUser.setEmail("admin@example.com");
            needSaveAdmin = true;
        }
        if (needSaveAdmin) {
            userRepo.save(adminUser);
        }

        // Regular User
        User regularUser = userRepo.findById("user-regular-001").orElse(new User());
        boolean needSaveUser = false;
        if (regularUser.getUserId() == null) {
            regularUser.setUserId("user-regular-001");
            regularUser.setSystemUserId(userSystemUserId);
            regularUser.setTenantId("default");
            regularUser.setDisplayName("Regular User");
            regularUser.setFirstName("Regular");
            regularUser.setLastName("User");
            regularUser.setIsActive(true);
            regularUser.setEmailVerified(true);
            regularUser.setLastLoginAt(now);
            needSaveUser = true;
        }
        // Ensure username/email are set (fix for null username issue)
        if (regularUser.getUsername() == null) {
            regularUser.setUsername("user");
            needSaveUser = true;
        }
        if (regularUser.getEmail() == null) {
            regularUser.setEmail("user@example.com");
            needSaveUser = true;
        }
        if (needSaveUser) {
            userRepo.save(regularUser);
        }

        // 4. UserTenant (Associations)
        if (!userTenantRepo.existsById("ut-admin-default")) {
            UserTenant utAdminDefault = new UserTenant();
            utAdminDefault.setId("ut-admin-default");
            utAdminDefault.setUserId("user-admin-001");
            utAdminDefault.setTenantId("default");
            utAdminDefault.setRoleInTenant("ADMIN");
            utAdminDefault.setIsDefault(true);
            utAdminDefault.setJoinedAt(now);
            userTenantRepo.save(utAdminDefault);
        }

        if (!userTenantRepo.existsById("ut-user-default")) {
            UserTenant utUserDefault = new UserTenant();
            utUserDefault.setId("ut-user-default");
            utUserDefault.setUserId("user-regular-001");
            utUserDefault.setTenantId("default");
            utUserDefault.setRoleInTenant("MEMBER");
            utUserDefault.setIsDefault(true);
            utUserDefault.setJoinedAt(now);
            userTenantRepo.save(utUserDefault);
        }

        if (!userTenantRepo.existsById("ut-admin-enterprise")) {
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
    }

}
