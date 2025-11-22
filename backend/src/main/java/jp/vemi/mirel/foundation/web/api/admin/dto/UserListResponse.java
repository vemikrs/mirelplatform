/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ページング対応ユーザーリストDTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {
    private List<AdminUserDto> users;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
