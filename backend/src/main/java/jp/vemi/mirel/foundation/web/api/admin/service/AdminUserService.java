/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.service;

import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.entity.UserTenant;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserTenantRepository;
import jp.vemi.mirel.foundation.web.api.admin.dto.AdminUserDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.UpdateUserRequest;
import jp.vemi.mirel.foundation.web.api.admin.dto.UserListResponse;
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
    private UserTenantRepository userTenantRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

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

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUserId(java.util.UUID.randomUUID().toString()); // ID を手動生成
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        // パスワードをハッシュ化してpasswordHashに保存
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getRoles() != null) {
            user.setRoles(String.join(",", request.getRoles()));
        }
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        user.setEmailVerified(false); // デフォルトは未認証

        user = userRepository.save(user);

        return convertToAdminUserDto(user);
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
}
