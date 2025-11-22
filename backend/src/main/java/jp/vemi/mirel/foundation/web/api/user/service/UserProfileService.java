/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.user.service;

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
import jp.vemi.mirel.foundation.web.api.user.dto.LicenseInfoDto;
import jp.vemi.mirel.foundation.web.api.user.dto.TenantInfoDto;
import jp.vemi.mirel.foundation.web.api.user.dto.UpdatePasswordRequest;
import jp.vemi.mirel.foundation.web.api.user.dto.UpdateProfileRequest;
import jp.vemi.mirel.foundation.web.api.user.dto.UserProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ユーザープロフィール管理サービス.
 */
@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private ApplicationLicenseRepository licenseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * ユーザープロフィール取得
     */
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String userId) {
        logger.info("Getting user profile: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant currentTenant = null;
        if (user.getTenantId() != null) {
            currentTenant = tenantRepository.findById(user.getTenantId()).orElse(null);
        }

        return UserProfileDto.builder()
            .userId(user.getUserId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .isActive(user.getIsActive())
            .emailVerified(user.getEmailVerified())
            .lastLoginAt(user.getLastLoginAt())
            .currentTenant(currentTenant != null ? TenantInfoDto.builder()
                .tenantId(currentTenant.getTenantId())
                .tenantName(currentTenant.getTenantName())
                .displayName(currentTenant.getDisplayName())
                .build() : null)
            .build();
    }

    /**
     * プロフィール更新
     */
    @Transactional
    public UserProfileDto updateProfile(String userId, UpdateProfileRequest request) {
        logger.info("Updating user profile: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        user = userRepository.save(user);

        logger.info("User profile updated successfully: {}", userId);

        return getUserProfile(userId);
    }

    /**
     * パスワード更新
     */
    @Transactional
    public void updatePassword(String userId, UpdatePasswordRequest request) {
        logger.info("Updating password for user: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // SystemUser取得
        SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId())
            .orElseThrow(() -> new RuntimeException("SystemUser not found"));

        // 現在のパスワード検証
        if (!passwordEncoder.matches(request.getCurrentPassword(), systemUser.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // 新しいパスワードをハッシュ化
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        systemUser.setPasswordHash(newPasswordHash);
        systemUser.setPasswordUpdatedAt(LocalDateTime.now());

        systemUserRepository.save(systemUser);

        logger.info("Password updated successfully for user: {}", userId);
    }

    /**
     * 所属テナント一覧取得
     */
    @Transactional(readOnly = true)
    public List<TenantInfoDto> getUserTenants(String userId) {
        logger.info("Getting tenants for user: {}", userId);

        List<UserTenant> userTenants = userTenantRepository.findByUserId(userId);

        return userTenants.stream()
            .map(ut -> {
                Tenant tenant = tenantRepository.findById(ut.getTenantId()).orElse(null);
                if (tenant == null) {
                    return null;
                }
                return TenantInfoDto.builder()
                    .tenantId(tenant.getTenantId())
                    .tenantName(tenant.getTenantName())
                    .displayName(tenant.getDisplayName())
                    .roleInTenant(ut.getRoleInTenant())
                    .isDefault(ut.getIsDefault())
                    .build();
            })
            .filter(t -> t != null)
            .collect(Collectors.toList());
    }

    /**
     * 有効ライセンス一覧取得
     */
    @Transactional(readOnly = true)
    public List<LicenseInfoDto> getUserLicenses(String userId, String tenantId) {
        logger.info("Getting licenses for user: {}, tenant: {}", userId, tenantId);

        List<ApplicationLicense> licenses = licenseRepository
            .findEffectiveLicenses(userId, tenantId, Instant.now());

        return licenses.stream()
            .map(license -> LicenseInfoDto.builder()
                .id(license.getId())
                .subjectType(license.getSubjectType())
                .subjectId(license.getSubjectId())
                .applicationId(license.getApplicationId())
                .tier(license.getTier())
                .grantedAt(license.getGrantedAt())
                .expiresAt(license.getExpiresAt())
                .build())
            .collect(Collectors.toList());
    }
}
