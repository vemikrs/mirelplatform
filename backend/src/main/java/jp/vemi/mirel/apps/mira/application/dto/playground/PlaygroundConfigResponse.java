/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.dto.playground;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * プレイグラウンド設定レスポンス DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaygroundConfigResponse {

    /** 利用可能なモデルIDリスト */
    private List<ModelOption> models;

    /** デフォルトパラメータ */
    private DefaultParams defaultParams;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelOption {
        private String id;
        private String name;
        private String provider;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DefaultParams {
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Integer maxTokens;
        private String systemPrompt;
    }
}
