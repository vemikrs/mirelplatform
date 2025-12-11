/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 管理者用ユーザー詳細DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private String userId;
    private String email;
    private String username;
    private String displayName;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private Boolean emailVerified;
    private String roles;
    private String avatarUrl;
    private Instant lastLoginAt;
    private Instant createdAt;
    private List<UserTenantInfo> tenants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTenantInfo {
        private String tenantId;
        private String tenantName;
        private String roleInTenant;
        private Boolean isDefault;
    }
}
