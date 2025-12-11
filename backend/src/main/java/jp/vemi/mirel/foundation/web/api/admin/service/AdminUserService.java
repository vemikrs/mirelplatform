/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.service;

import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.entity.UserTenant;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserTenantRepository;
import jp.vemi.mirel.foundation.config.AppProperties;
import jp.vemi.mirel.foundation.service.EmailService;
import jp.vemi.mirel.foundation.service.OtpService;
import jp.vemi.mirel.foundation.web.api.admin.dto.AdminUserDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.UpdateUserRequest;
import jp.vemi.mirel.foundation.web.api.admin.dto.UserListResponse;
import jp.vemi.mirel.foundation.web.api.admin.dto.UserTenantAssignmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理者用ユーザー管理サービス.
 */
@Service
public class AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AppProperties appProperties;

    /**
     * ユーザー一覧取得（ページング対応）
     */
    @Transactional(readOnly = true)
    public UserListResponse listUsers(int page, int size, String query, String role, Boolean active) {
        logger.info("List users: page={}, size={}, query={}, role={}, active={}",
                page, size, query, role, active);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());

        org.springframework.data.jpa.domain.Specification<User> spec = (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isEmpty()) {
                String likePattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), likePattern),
                        cb.like(cb.lower(root.get("email")), likePattern),
                        cb.like(cb.lower(root.get("displayName")), likePattern)));
            }

            if (role != null && !role.isEmpty()) {
                // simple contains check for CSV roles
                predicates.add(cb.like(root.get("roles"), "%" + role + "%"));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<AdminUserDto> userDtos = userPage.getContent().stream()
                .map(this::convertToAdminUserDto)
                .collect(Collectors.toList());

        return UserListResponse.builder()
                .users(userDtos)
                .page(page)
                .size(size)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    /**
     * ユーザー詳細取得
     */
    @Transactional(readOnly = true)
    public AdminUserDto getUserById(String userId) {
        logger.info("Get user by id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return convertToAdminUserDto(user);
    }

    /**
     * ユーザー更新
     */
    @Transactional
    public AdminUserDto updateUser(String userId, UpdateUserRequest request) {
        logger.info("Update user: {}", userId);

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
        if (request.getRoles() != null) {
            user.setRoles(request.getRoles());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        user = userRepository.save(user);

        logger.info("User updated successfully: {}", userId);

        return convertToAdminUserDto(user);
    }

    /**
     * UserをAdminUserDtoに変換
     */
    private AdminUserDto convertToAdminUserDto(User user) {
        List<UserTenant> userTenants = userTenantRepository.findByUserId(user.getUserId());

        List<AdminUserDto.UserTenantInfo> tenantInfos = new ArrayList<>();
        for (UserTenant ut : userTenants) {
            Tenant tenant = tenantRepository.findById(ut.getTenantId()).orElse(null);
            if (tenant != null) {
                tenantInfos.add(AdminUserDto.UserTenantInfo.builder()
                        .tenantId(tenant.getTenantId())
                        .tenantName(tenant.getTenantName())
                        .roleInTenant(ut.getRoleInTenant())
                        .isDefault(ut.getIsDefault())
                        .build());
            }
        }

        // SystemUserからavatarUrlを取得
        String avatarUrl = null;
        if (user.getSystemUserId() != null) {
            SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId()).orElse(null);
            if (systemUser != null) {
                avatarUrl = systemUser.getAvatarUrl();
            }
        }

        return AdminUserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .roles(user.getRoles())
                .avatarUrl(avatarUrl)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreateDate() != null ? Instant.ofEpochMilli(user.getCreateDate().getTime()) : null)
                .tenants(tenantInfos)
                .build();
    }

    /**
     * ユーザー作成
     */
    @Transactional
    public AdminUserDto createUser(jp.vemi.mirel.foundation.web.api.admin.dto.CreateUserRequest request) {
        logger.info("Create user: {}", request.getUsername());

        // 既存ユーザーチェック（User）
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        // 既存ユーザーチェック（SystemUser）
        if (systemUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists in SystemUser");
        }
        
        if (systemUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists in SystemUser");
        }

        // 1. SystemUser作成
        SystemUser systemUser = new SystemUser();
        systemUser.setId(UUID.randomUUID());
        systemUser.setUsername(request.getUsername());
        systemUser.setEmail(request.getEmail());
        // 管理者作成ユーザーは初回パスワードをセットアップリンク経由で設定するため、
        // ここではダミーハッシュを設定（セットアップ完了時に上書きされる）
        systemUser.setPasswordHash(passwordEncoder.encode("TEMP_PASSWORD_" + UUID.randomUUID()));
        systemUser.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        systemUser.setEmailVerified(false); // 管理者作成ユーザーは未認証
        systemUser.setCreatedByAdmin(true); // 管理者作成フラグをセット
        systemUser = systemUserRepository.save(systemUser);
        
        logger.info("SystemUser created: id={}, username={}", systemUser.getId(), systemUser.getUsername());

        // 2. User作成（SystemUserと紐付け）
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setSystemUserId(systemUser.getId()); // SystemUserと紐付け
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(systemUser.getPasswordHash()); // SystemUserと同じダミーハッシュ
        user.setDisplayName(request.getDisplayName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getRoles() != null) {
            user.setRoles(String.join(",", request.getRoles()));
        }
        user.setIsActive(systemUser.getIsActive());
        user.setEmailVerified(false); // SystemUserと同期

        user = userRepository.save(user);
        
        logger.info("User created and linked to SystemUser: userId={}, systemUserId={}", 
                user.getUserId(), user.getSystemUserId());

        // 3. アカウントセットアップトークン作成とメール送信
        try {
            String setupToken = otpService.createAccountSetupToken(systemUser.getId(), systemUser.getEmail());
            sendAccountSetupEmail(user, setupToken);
            logger.info("Account setup email sent: email={}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send account setup email: email={}", user.getEmail(), e);
            // メール送信失敗してもユーザー作成は成功とする
        }

        return convertToAdminUserDto(user);
    }

    /**
     * アカウントセットアップメール送信
     */
    private void sendAccountSetupEmail(User user, String setupToken) {
        String setupLink = String.format("%s/auth/setup-account?token=%s",
                appProperties.getBaseUrl(), setupToken);

        Map<String, Object> variables = Map.of(
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "setupLink", setupLink);

        emailService.sendTemplateEmail(
                user.getEmail(),
                "アカウント作成完了 - パスワード設定のご案内",
                "account-setup",
                variables);
    }

    /**
     * ユーザー削除
     */
    @Transactional
    public void deleteUser(String userId) {
        logger.info("Delete user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }

        userRepository.deleteById(userId);
    }

    /**
     * ユーザーのテナント割り当てを更新
     */
    @Transactional
    public AdminUserDto updateUserTenants(String userId, UserTenantAssignmentRequest request) {
        logger.info("Update user tenants: userId={}, tenants={}", userId, request.getTenants().size());

        // ユーザーの存在確認
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 既存のテナント割り当てを削除
        List<UserTenant> existingAssignments = userTenantRepository.findByUserId(userId);
        if (!existingAssignments.isEmpty()) {
            userTenantRepository.deleteAll(existingAssignments);
        }

        // デフォルトテナントの数をチェック
        long defaultCount = request.getTenants().stream()
                .filter(UserTenantAssignmentRequest.TenantAssignment::getIsDefault)
                .count();
        if (defaultCount != 1) {
            throw new RuntimeException("Exactly one default tenant is required");
        }

        // 新しいテナント割り当てを作成
        for (UserTenantAssignmentRequest.TenantAssignment assignment : request.getTenants()) {
            // テナントの存在確認
            if (!tenantRepository.existsById(assignment.getTenantId())) {
                throw new RuntimeException("Tenant not found: " + assignment.getTenantId());
            }

            UserTenant userTenant = new UserTenant();
            userTenant.setUserId(userId);
            userTenant.setTenantId(assignment.getTenantId());
            userTenant.setRoleInTenant(assignment.getRoleInTenant());
            userTenant.setIsDefault(assignment.getIsDefault());
            userTenantRepository.save(userTenant);
        }

        // 更新後のユーザー情報を返す
        return convertToAdminUserDto(user);
    }
}
