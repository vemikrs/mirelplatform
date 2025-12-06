/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String displayName;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Boolean isActive;
}
