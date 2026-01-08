/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.bootstrap.service;

import jp.vemi.framework.exeption.MirelValidationException;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration.MigrationType;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration.MigrationStatus;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.entity.UserTenant;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemMigrationRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserTenantRepository;
import jp.vemi.mirel.foundation.feature.tenant.dto.TenantPlan;
import jp.vemi.mirel.foundation.feature.tenant.dto.TenantStatus;
import jp.vemi.mirel.foundation.web.api.admin.dto.AdminUserDto;
import jp.vemi.mirel.foundation.web.api.bootstrap.dto.CreateInitialAdminRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Bootstrapサービス.
 * 
 * 初期管理者作成とシステムテナント作成を行います。
 */
@Service
@RequiredArgsConstructor
public class BootstrapService {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapService.class);

    private static final String MIGRATION_ID_BOOTSTRAP = "BOOTSTRAP_V1";
    private static final String SYSTEM_TENANT_ID = "system";
    private static final String SYSTEM_TENANT_NAME = "System Tenant";

    private final BootstrapTokenService tokenService;
    private final SystemMigrationRepository migrationRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SystemUserRepository systemUserRepository;
    private final UserTenantRepository userTenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Bootstrap可能かどうかを確認
     */
    public boolean isBootstrapAvailable() {
        try {
            return !migrationRepository.isCompleted(MIGRATION_ID_BOOTSTRAP);
        } catch (Exception e) {
            // テーブルが存在しない場合はBootstrap可能
            return true;
        }
    }

    /**
     * 初期管理者を作成
     */
    @Transactional
    public AdminUserDto createInitialAdmin(CreateInitialAdminRequest request) {
        logger.info("Creating initial admin user");

        // 1. Bootstrap可能状態チェック
        if (!isBootstrapAvailable()) {
            throw new MirelValidationException("Bootstrap already completed. Initial admin already exists.");
        }

        // 2. トークン検証
        if (!tokenService.validateToken(request.getToken())) {
            throw new MirelValidationException("Invalid bootstrap token");
        }

        // 3. 既存ユーザーチェック
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new MirelValidationException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new MirelValidationException("Email already exists");
        }
        if (systemUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new MirelValidationException("Username already exists in SystemUser");
        }
        if (systemUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new MirelValidationException("Email already exists in SystemUser");
        }

        // 4. システムテナント作成（存在しない場合）
        Tenant systemTenant = getOrCreateSystemTenant();

        // 5. SystemUser作成
        SystemUser systemUser = new SystemUser();
        systemUser.setId(UUID.randomUUID());
        systemUser.setUsername(request.getUsername());
        systemUser.setEmail(request.getEmail());
        systemUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        systemUser.setIsActive(true);
        systemUser.setEmailVerified(true); // Bootstrap経由は検証済み扱い
        systemUser.setCreatedByAdmin(true);
        systemUser = systemUserRepository.save(systemUser);

        logger.info("SystemUser created: id={}", systemUser.getId());

        // 6. User作成
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setSystemUserId(systemUser.getId());
        user.setTenantId(systemTenant.getTenantId());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(systemUser.getPasswordHash());
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setRoles("ADMIN"); // システム管理者ロール
        user.setIsActive(true);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        logger.info("User created: id={}", user.getUserId());

        // 7. UserTenant作成
        UserTenant userTenant = new UserTenant();
        userTenant.setUserId(user.getUserId());
        userTenant.setTenantId(systemTenant.getTenantId());
        userTenant.setRoleInTenant("OWNER");
        userTenant.setIsDefault(true);
        userTenant.setJoinedAt(Instant.now());
        userTenantRepository.save(userTenant);

        logger.info("UserTenant created: userId={}, tenantId={}", user.getUserId(), systemTenant.getTenantId());

        // 8. Bootstrap完了記録
        recordBootstrapComplete(user.getUserId());

        // 9. トランザクション成功後にトークンファイル削除イベント発行
        eventPublisher.publishEvent(new BootstrapCompletedEvent(this));

        logger.info("Bootstrap completed successfully");

        return convertToAdminUserDto(user);
    }

    /**
     * システムテナントを取得または作成
     */
    private Tenant getOrCreateSystemTenant() {
        return tenantRepository.findById(SYSTEM_TENANT_ID)
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setTenantId(SYSTEM_TENANT_ID);
                    tenant.setTenantName(SYSTEM_TENANT_NAME);
                    tenant.setDisplayName(SYSTEM_TENANT_NAME);
                    tenant.setDomain("system");
                    tenant.setPlan(TenantPlan.ENTERPRISE);
                    tenant.setStatus(TenantStatus.ACTIVE);
                    tenant.setIsActive(true);
                    tenant.setCreateUserId("BOOTSTRAP");
                    tenant.setCreateDate(new Date());
                    tenant = tenantRepository.save(tenant);
                    logger.info("System tenant created: id={}", tenant.getTenantId());
                    return tenant;
                });
    }

    /**
     * Bootstrap完了を記録
     */
    private void recordBootstrapComplete(String adminUserId) {
        SystemMigration migration = new SystemMigration();
        migration.setId(MIGRATION_ID_BOOTSTRAP);
        migration.setMigrationType(MigrationType.BOOTSTRAP);
        migration.setVersion("1.0.0");
        migration.setStatus(MigrationStatus.COMPLETED);
        migration.setAppliedAt(LocalDateTime.now());
        migration.setAppliedBy(adminUserId);
        migration.setDetails("Initial admin created: " + adminUserId);
        migrationRepository.save(migration);
    }

    /**
     * UserをAdminUserDtoに変換
     */
    private AdminUserDto convertToAdminUserDto(User user) {
        return AdminUserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .roles(user.getRoles())
                .createdAt(user.getCreateDate() != null ? Instant.ofEpochMilli(user.getCreateDate().getTime()) : null)
                .build();
    }

    /**
     * Bootstrap完了イベント
     */
    public static class BootstrapCompletedEvent {
        private final Object source;

        public BootstrapCompletedEvent(Object source) {
            this.source = source;
        }

        public Object getSource() {
            return source;
        }
    }

    /**
     * Bootstrap完了後にトークンファイルを削除
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBootstrapCompleted(BootstrapCompletedEvent event) {
        tokenService.deleteTokenFile();
        logger.info("Bootstrap token file deleted after successful commit");
    }
}
