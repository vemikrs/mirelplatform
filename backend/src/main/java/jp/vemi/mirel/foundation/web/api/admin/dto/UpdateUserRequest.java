/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.Data;

/**
 * ユーザー更新リクエストDTO.
 */
@Data
public class UpdateUserRequest {
    private String displayName;
    private String firstName;
    private String lastName;
    private String roles;
    private Boolean isActive;
}
