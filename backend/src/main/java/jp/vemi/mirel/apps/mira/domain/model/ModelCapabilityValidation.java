/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * モデル機能バリデーション結果.
 */
@Data
@Builder
public class ModelCapabilityValidation {
    
    /** バリデーション成功フラグ */
    private final boolean valid;
    
    /** エラーメッセージ一覧 */
    @Builder.Default
    private final List<String> errors = new ArrayList<>();
    
    /** 警告メッセージ一覧 */
    @Builder.Default
    private final List<String> warnings = new ArrayList<>();
    
    /**
     * 成功結果を生成.
     */
    public static ModelCapabilityValidation success() {
        return ModelCapabilityValidation.builder()
            .valid(true)
            .build();
    }
    
    /**
     * 失敗結果を生成.
     * 
     * @param errors エラーメッセージ一覧
     */
    public static ModelCapabilityValidation failure(List<String> errors) {
        return ModelCapabilityValidation.builder()
            .valid(false)
            .errors(errors)
            .build();
    }
    
    /**
     * 単一エラーで失敗結果を生成.
     * 
     * @param error エラーメッセージ
     */
    public static ModelCapabilityValidation failure(String error) {
        return ModelCapabilityValidation.builder()
            .valid(false)
            .errors(List.of(error))
            .build();
    }
    
    /**
     * エラーメッセージを結合して取得.
     */
    public String getErrorMessage() {
        return String.join("; ", errors);
    }
}
