/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.user.dto;

import lombok.Data;

/**
 * プロフィール更新リクエストDTO.
 */
@Data
public class UpdateProfileRequest {
    private String username;
    private String displayName;
    private String firstName;
    private String lastName;
    private String bio;
    private String phoneNumber;
    private String preferredLanguage;
    private String timezone;
}
