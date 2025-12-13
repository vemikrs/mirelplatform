/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * モデルごとの機能サポート状況を管理するレジストリ.
 * <p>
 * 疎結合な設計: モデル名のパターンマッチングにより、
 * 新しいモデルが追加されても柔軟に対応可能。
 * </p>
 * 
 * <p>
 * 使用例:
 * <pre>
 * Set&lt;ModelCapability&gt; caps = registry.getCapabilities("openai/gpt-4o-mini");
 * boolean supportsTools = caps.contains(ModelCapability.TOOL_CALLING);
 * </pre>
 * </p>
 */
@Slf4j
@Component
public class ModelCapabilityRegistry {
    
    /**
     * モデルパターンと機能のマッピング.
     * <p>
     * LinkedHashMap を使用してパターンの優先度順（最初にマッチしたものが使用される）を保証。
     * </p>
     */
    private static final Map<Pattern, Set<ModelCapability>> MODEL_CAPABILITIES = new LinkedHashMap<>();
    
    static {
        // OpenAI GPT-4o 系 (フル機能)
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(openai/)?(gpt-4o|gpt-5).*"),
            EnumSet.of(
                ModelCapability.TOOL_CALLING,
                ModelCapability.MULTIMODAL_INPUT,
                ModelCapability.STREAMING,
                ModelCapability.JSON_MODE,
                ModelCapability.LONG_CONTEXT
            )
        );
        
        // OpenAI GPT-4 系 (マルチモーダルなし)
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(openai/)?gpt-4(?!o).*"),
            EnumSet.of(
                ModelCapability.TOOL_CALLING,
                ModelCapability.STREAMING,
                ModelCapability.JSON_MODE
            )
        );
        
        // OpenAI GPT-3.5 系
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(openai/)?gpt-3\\.5.*"),
            EnumSet.of(
                ModelCapability.TOOL_CALLING,
                ModelCapability.STREAMING
            )
        );
        
        // OpenAI o1/o3/o4 系 (推論モデル - ツール呼び出し制限あり)
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(openai/)?o[134].*"),
            EnumSet.of(
                ModelCapability.STREAMING,
                ModelCapability.LONG_CONTEXT
                // NOTE: o1/o3/o4 はツール呼び出しに制限がある場合がある
            )
        );
        
        // Meta Llama 3.3 系 (ツール呼び出しに問題あり)
        // content: null が必須で Spring AI との相性が悪い
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(meta/)?llama-3\\.3.*"),
            EnumSet.of(
                ModelCapability.STREAMING,
                ModelCapability.LONG_CONTEXT
                // TOOL_CALLING は意図的に除外 (content:null 問題)
                // MULTIMODAL_INPUT は除外 (テキストのみ)
            )
        );
        
        // Meta Llama 3.2 Vision 系 (マルチモーダル対応)
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(meta/)?llama-3\\.2.*vision.*"),
            EnumSet.of(
                ModelCapability.MULTIMODAL_INPUT,
                ModelCapability.STREAMING
            )
        );
        
        // Meta Llama 3.2 系 (テキストのみ)
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(meta/)?llama-3\\.2.*"),
            EnumSet.of(
                ModelCapability.STREAMING
            )
        );
        
        // Meta Llama 3.1 系
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(meta/)?llama-3\\.1.*"),
            EnumSet.of(
                ModelCapability.STREAMING,
                ModelCapability.LONG_CONTEXT
            )
        );
        
        // Microsoft Phi 系 (マルチモーダル対応のものあり)
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(microsoft/)?phi-4-multimodal.*"),
            EnumSet.of(
                ModelCapability.MULTIMODAL_INPUT,
                ModelCapability.STREAMING
            )
        );
        
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(microsoft/)?phi.*"),
            EnumSet.of(
                ModelCapability.STREAMING
            )
        );
        
        // Anthropic Claude 系
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(anthropic/)?claude.*"),
            EnumSet.of(
                ModelCapability.TOOL_CALLING,
                ModelCapability.MULTIMODAL_INPUT,
                ModelCapability.STREAMING,
                ModelCapability.LONG_CONTEXT
            )
        );
        
        // Mistral 系
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(mistral/)?mistral.*"),
            EnumSet.of(
                ModelCapability.TOOL_CALLING,
                ModelCapability.STREAMING
            )
        );
        
        // Cohere 系
        MODEL_CAPABILITIES.put(
            Pattern.compile("(?i)^(cohere/)?command.*"),
            EnumSet.of(
                ModelCapability.TOOL_CALLING,
                ModelCapability.STREAMING
            )
        );
        
        // デフォルト (マッチしない場合)
        MODEL_CAPABILITIES.put(
            Pattern.compile(".*"),
            EnumSet.of(
                ModelCapability.STREAMING
            )
        );
    }
    
    /**
     * 指定されたモデルがサポートする機能セットを取得.
     * 
     * @param modelName モデル名 (例: "openai/gpt-4o-mini", "meta/llama-3.3-70b-instruct")
     * @return サポートされる機能のセット
     */
    public Set<ModelCapability> getCapabilities(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            log.warn("Model name is null or empty, returning default capabilities");
            return EnumSet.of(ModelCapability.STREAMING);
        }
        
        for (Map.Entry<Pattern, Set<ModelCapability>> entry : MODEL_CAPABILITIES.entrySet()) {
            if (entry.getKey().matcher(modelName).matches()) {
                log.debug("Model '{}' matched pattern '{}', capabilities: {}", 
                    modelName, entry.getKey().pattern(), entry.getValue());
                return Collections.unmodifiableSet(entry.getValue());
            }
        }
        
        // Should not reach here due to .* pattern, but just in case
        return EnumSet.of(ModelCapability.STREAMING);
    }
    
    /**
     * 指定されたモデルが特定の機能をサポートしているか確認.
     * 
     * @param modelName モデル名
     * @param capability 確認する機能
     * @return サポートしている場合は true
     */
    public boolean supports(String modelName, ModelCapability capability) {
        return getCapabilities(modelName).contains(capability);
    }
    
    /**
     * 指定されたモデルがツール呼び出しをサポートしているか確認.
     * 
     * @param modelName モデル名
     * @return サポートしている場合は true
     */
    public boolean supportsToolCalling(String modelName) {
        return supports(modelName, ModelCapability.TOOL_CALLING);
    }
    
    /**
     * 指定されたモデルがマルチモーダル入力をサポートしているか確認.
     * 
     * @param modelName モデル名
     * @return サポートしている場合は true
     */
    public boolean supportsMultimodal(String modelName) {
        return supports(modelName, ModelCapability.MULTIMODAL_INPUT);
    }
}
