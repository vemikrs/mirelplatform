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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * ユーザー一覧取得（ページング対応）
     */
    @Transactional(readOnly = true)
    public UserListResponse listUsers(int page, int size, String query, String role, Boolean active) {
        logger.info("List users: page={}, size={}, query={}, role={}, active={}", 
            page, size, query, role, active);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
        
        // TODO: Implement filtering by query, role, active
        // For now, just get all users
        Page<User> userPage = userRepository.findAll(pageable);

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
            .createdAt(user.getCreateDate() != null ? 
                Instant.ofEpochMilli(user.getCreateDate().getTime()) : null)
            .tenants(tenantInfos)
            .build();
    }
}
