package jp.vemi.mirel.foundation.web.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OAuth2サインアップリクエストDTO.
 */
@Data
public class OAuth2SignupRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String firstName;

    private String lastName;

    private String tenantId;
}
