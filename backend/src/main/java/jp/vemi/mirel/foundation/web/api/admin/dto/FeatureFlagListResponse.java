/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * フィーチャーフラグ一覧レスポンス.
 */
@Data
@Builder
public class FeatureFlagListResponse {

    private List<FeatureFlagDto> features;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
