/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.model.ModelCapability;
import jp.vemi.mirel.apps.mira.domain.model.ModelCapabilityRegistry;
import jp.vemi.mirel.apps.mira.domain.model.ModelCapabilityValidation;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * モデル機能バリデーションサービス.
 * <p>
 * チャットリクエストで要求される機能と、
 * 使用されるモデルがサポートする機能の互換性を検証。
 * </p>
 * 
 * <p>
 * 疎結合設計:
 * <ul>
 *   <li>{@link ModelCapabilityRegistry} でモデル情報を管理</li>
 *   <li>このサービスはバリデーションロジックのみに集中</li>
 *   <li>新しいモデルや機能の追加はレジストリ側で対応</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelCapabilityValidator {
    
    private final ModelCapabilityRegistry capabilityRegistry;
    private final MiraAiProperties aiProperties;
    
    /**
     * チャットリクエストとモデルの互換性を検証（デフォルトモデル使用）.
     * 
     * @param request チャットリクエスト
     * @return バリデーション結果
     */
    public ModelCapabilityValidation validate(ChatRequest request) {
        String modelName = resolveModelName();
        return validateWithModel(request, modelName);
    }
    
    /**
     * チャットリクエストと指定モデルの互換性を検証.
     * 
     * @param request チャットリクエスト
     * @param modelName モデル名
     * @return バリデーション結果
     */
    public ModelCapabilityValidation validateWithModel(ChatRequest request, String modelName) {
        List<String> errors = new ArrayList<>();
        
        // 1. Web検索 (ツール呼び出し) のバリデーション
        if (Boolean.TRUE.equals(request.getWebSearchEnabled())) {
            if (!capabilityRegistry.supportsToolCalling(modelName)) {
                errors.add(buildCapabilityError(
                    modelName, 
                    ModelCapability.TOOL_CALLING,
                    "Web検索機能",
                    "Web検索を利用するには、GPT-4o-mini などのツール呼び出し対応モデルに切り替えてください。"
                ));
            }
        }
        
        // 2. マルチモーダル入力のバリデーション (将来実装)
        // 現在は ChatRequest にマルチモーダルフラグがないため、
        // メッセージ内容から画像/音声を検出する場合に拡張予定
        if (hasMultimodalContent(request)) {
            if (!capabilityRegistry.supportsMultimodal(modelName)) {
                errors.add(buildCapabilityError(
                    modelName,
                    ModelCapability.MULTIMODAL_INPUT,
                    "画像・音声入力",
                    "マルチモーダル機能を利用するには、GPT-4o などの対応モデルに切り替えてください。"
                ));
            }
        }
        
        // 結果を返す
        if (errors.isEmpty()) {
            return ModelCapabilityValidation.success();
        } else {
            log.warn("Model capability validation failed for model '{}': {}", modelName, errors);
            return ModelCapabilityValidation.failure(errors);
        }
    }
    
    /**
     * 使用されるモデル名を解決.
     * <p>
     * 将来的にはテナント別設定やリクエスト内の指定を考慮。
     * </p>
     */
    private String resolveModelName() {
        // 現在は設定ファイルのデフォルトモデルを使用
        // TODO: テナント別設定、リクエスト内の指定を優先する実装
        var githubModels = aiProperties.getGithubModels();
        if (githubModels != null && githubModels.getModel() != null) {
            return githubModels.getModel();
        }
        
        // フォールバック
        return "unknown";
    }
    
    /**
     * マルチモーダルコンテンツが含まれているか検出.
     * <p>
     * 現在は簡易的な検出。将来的にはより精密な検出を実装。
     * </p>
     */
    private boolean hasMultimodalContent(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().getContent() == null) {
            return false;
        }
        
        String content = request.getMessage().getContent();
        
        // Base64 画像データの簡易検出
        if (content.contains("data:image/") || content.contains("data:audio/")) {
            return true;
        }
        
        // 画像URLパターンの簡易検出 (将来拡張)
        // if (content.matches(".*\\.(jpg|jpeg|png|gif|webp).*")) {
        //     return true;
        // }
        
        return false;
    }
    
    /**
     * 機能エラーメッセージを構築.
     */
    private String buildCapabilityError(
            String modelName, 
            ModelCapability capability,
            String featureName,
            String suggestion) {
        
        return String.format(
            "現在のモデル「%s」は%s（%s）をサポートしていません。%s",
            getDisplayModelName(modelName),
            featureName,
            capability.getDisplayName(),
            suggestion
        );
    }
    
    /**
     * モデル名を表示用にフォーマット.
     */
    private String getDisplayModelName(String modelName) {
        if (modelName == null) {
            return "不明なモデル";
        }
        
        // "meta/llama-3.3-70b-instruct" -> "Llama 3.3 70B"
        // "openai/gpt-4o-mini" -> "GPT-4o-mini"
        String name = modelName;
        
        // プレフィックス除去
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        
        return name;
    }
}
