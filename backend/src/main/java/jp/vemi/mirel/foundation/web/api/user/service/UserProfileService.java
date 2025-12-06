/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import jp.vemi.mirel.foundation.service.AvatarService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Autowired
    private AvatarService avatarService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // SystemUserから追加情報を取得
        SystemUser systemUser = null;
        if (user.getSystemUserId() != null) {
            systemUser = systemUserRepository.findById(user.getSystemUserId()).orElse(null);
        }

        // attributes JSONからカスタム属性を取得
        Map<String, String> attributes = parseAttributes(user.getAttributes());

        return UserProfileDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(systemUser != null ? systemUser.getAvatarUrl() : null)
                .bio(attributes.get("bio"))
                .phoneNumber(attributes.get("phoneNumber"))
                .preferredLanguage(attributes.get("preferredLanguage"))
                .timezone(attributes.get("timezone"))
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .currentTenant(currentTenant != null ? TenantInfoDto.builder()
                        .tenantId(currentTenant.getTenantId())
                        .tenantName(currentTenant.getTenantName())
                        .displayName(currentTenant.getDisplayName())
                        .build() : null)
                .roles(user.getRoles() != null && !user.getRoles().isEmpty()
                        ? java.util.Arrays.asList(user.getRoles().split("\\|"))
                        : java.util.Collections.emptyList())
                .oauth2Provider(systemUser != null ? systemUser.getOauth2Provider() : null)
                .hasPassword(systemUser != null && systemUser.getPasswordHash() != null 
                        && !systemUser.getPasswordHash().isEmpty()
                        && !systemUser.getPasswordHash().startsWith("$OAUTH2$"))
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

        // ユーザー名の変更
        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            // 重複チェック
            Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getUserId().equals(userId)) {
                throw new IllegalArgumentException("このユーザー名は既に使用されています");
            }
            user.setUsername(request.getUsername());
            
            // SystemUserも更新
            if (user.getSystemUserId() != null) {
                systemUserRepository.findById(user.getSystemUserId()).ifPresent(su -> {
                    su.setUsername(request.getUsername());
                    systemUserRepository.save(su);
                });
            }
        }

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        // カスタム属性をattributes JSONに保存
        Map<String, String> attributes = parseAttributes(user.getAttributes());
        if (request.getBio() != null) {
            attributes.put("bio", request.getBio());
        }
        if (request.getPhoneNumber() != null) {
            attributes.put("phoneNumber", request.getPhoneNumber());
        }
        if (request.getPreferredLanguage() != null) {
            attributes.put("preferredLanguage", request.getPreferredLanguage());
        }
        if (request.getTimezone() != null) {
            attributes.put("timezone", request.getTimezone());
        }
        user.setAttributes(serializeAttributes(attributes));

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

    /**
     * メールアドレス更新（OTP検証済み前提）
     */
    @Transactional
    public UserProfileDto updateEmail(String userId, String newEmail) {
        logger.info("Updating email for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 重複チェック
        Optional<User> existingUser = userRepository.findByEmail(newEmail);
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("このメールアドレスは既に使用されています");
        }

        user.setEmail(newEmail);
        user.setEmailVerified(true);
        userRepository.save(user);

        // SystemUserも更新
        if (user.getSystemUserId() != null) {
            systemUserRepository.findById(user.getSystemUserId()).ifPresent(su -> {
                su.setEmail(newEmail);
                su.setEmailVerified(true);
                systemUserRepository.save(su);
            });
        }

        logger.info("Email updated successfully for user: {}", userId);
        return getUserProfile(userId);
    }

    /**
     * アバター画像アップロード
     */
    @Transactional
    public String uploadAvatar(String userId, MultipartFile file) throws IOException {
        logger.info("Uploading avatar for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSystemUserId() == null) {
            throw new RuntimeException("SystemUser not found");
        }

        SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId())
                .orElseThrow(() -> new RuntimeException("SystemUser not found"));

        // ファイルサイズチェック（5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("ファイルサイズは5MB以下にしてください");
        }

        // MIMEタイプチェック
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("画像ファイルのみアップロード可能です");
        }

        // アバターを保存
        byte[] imageBytes = file.getBytes();
        String avatarUrl = avatarService.saveAvatarFromBytes(imageBytes, systemUser.getId(), 
                getExtensionFromContentType(contentType));

        if (avatarUrl != null) {
            systemUser.setAvatarUrl(avatarUrl);
            systemUserRepository.save(systemUser);
        }

        logger.info("Avatar uploaded successfully for user: {}", userId);
        return avatarUrl;
    }

    /**
     * アバター画像削除
     */
    @Transactional
    public void deleteAvatar(String userId) {
        logger.info("Deleting avatar for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSystemUserId() == null) {
            return;
        }

        SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId())
                .orElse(null);
        if (systemUser == null) {
            return;
        }

        avatarService.deleteAvatar(systemUser.getId());
        systemUser.setAvatarUrl(null);
        systemUserRepository.save(systemUser);

        logger.info("Avatar deleted successfully for user: {}", userId);
    }

    /**
     * GitHub連携解除
     */
    @Transactional
    public void unlinkGitHub(String userId) {
        logger.info("Unlinking GitHub for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSystemUserId() == null) {
            throw new RuntimeException("SystemUser not found");
        }

        SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId())
                .orElseThrow(() -> new RuntimeException("SystemUser not found"));

        // パスワードが設定されていない場合は連携解除不可
        if (systemUser.getPasswordHash() == null || systemUser.getPasswordHash().isEmpty()
                || systemUser.getPasswordHash().startsWith("$OAUTH2$")) {
            throw new IllegalStateException("パスワードを設定してからGitHub連携を解除してください");
        }

        systemUser.setOauth2Provider(null);
        systemUser.setOauth2ProviderId(null);
        systemUserRepository.save(systemUser);

        logger.info("GitHub unlinked successfully for user: {}", userId);
    }

    /**
     * パスワードレスログイン設定（パスワード削除）
     */
    @Transactional
    public void enablePasswordlessLogin(String userId) {
        logger.info("Enabling passwordless login for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSystemUserId() == null) {
            throw new RuntimeException("SystemUser not found");
        }

        SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId())
                .orElseThrow(() -> new RuntimeException("SystemUser not found"));

        // メールが検証済みでない場合は不可
        if (!Boolean.TRUE.equals(systemUser.getEmailVerified())) {
            throw new IllegalStateException("メールアドレスを検証してからパスワードレスログインを有効にしてください");
        }

        // パスワードを無効化（OAuthプレフィックス付き）
        systemUser.setPasswordHash("$OAUTH2$PASSWORDLESS$" + java.util.UUID.randomUUID().toString());
        systemUserRepository.save(systemUser);

        logger.info("Passwordless login enabled for user: {}", userId);
    }

    /**
     * パスワード設定（パスワードレスから通常に戻す）
     */
    @Transactional
    public void setPassword(String userId, String newPassword) {
        logger.info("Setting password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSystemUserId() == null) {
            throw new RuntimeException("SystemUser not found");
        }

        SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId())
                .orElseThrow(() -> new RuntimeException("SystemUser not found"));

        String newPasswordHash = passwordEncoder.encode(newPassword);
        systemUser.setPasswordHash(newPasswordHash);
        systemUser.setPasswordUpdatedAt(LocalDateTime.now());
        systemUserRepository.save(systemUser);

        logger.info("Password set successfully for user: {}", userId);
    }

    // ヘルパーメソッド

    private Map<String, String> parseAttributes(String attributesJson) {
        if (attributesJson == null || attributesJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(attributesJson, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse attributes JSON", e);
            return new HashMap<>();
        }
    }

    private String serializeAttributes(Map<String, String> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize attributes", e);
            return "{}";
        }
    }

    private String getExtensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
