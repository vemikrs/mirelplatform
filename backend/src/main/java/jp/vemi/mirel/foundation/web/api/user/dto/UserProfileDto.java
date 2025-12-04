/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ユーザープロフィールDTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private Boolean emailVerified;
    private Instant lastLoginAt;
    private TenantInfoDto currentTenant;
    private java.util.List<String> roles;
}
