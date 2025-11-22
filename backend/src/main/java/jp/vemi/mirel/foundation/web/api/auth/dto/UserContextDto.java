/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザコンテキストDTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContextDto {
    private UserDto user;
    private TenantContextDto currentTenant;
}
