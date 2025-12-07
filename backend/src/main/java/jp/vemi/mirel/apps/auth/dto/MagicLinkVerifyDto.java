/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * マジックリンク検証DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagicLinkVerifyDto {
    /**
     * マジックリンクトークン
     */
    private String token;
}
