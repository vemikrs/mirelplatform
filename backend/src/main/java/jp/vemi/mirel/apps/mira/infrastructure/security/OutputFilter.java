/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.config.MiraAiProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 出力フィルター.
 * <p>
 * AI の応答からシステムプロンプトの漏洩や機密情報の露出を防止する。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutputFilter {

    private final MiraAiProperties properties;
    private final PiiMasker piiMasker;

    /**
     * 機密情報パターン.
     */
    private static final List<SensitivePattern> SENSITIVE_PATTERNS = List.of(
            // システムプロンプトの断片
            new SensitivePattern(
                    Pattern.compile("(?i)system\\s*prompt\\s*:?\\s*", Pattern.DOTALL),
                    "system_prompt_reference"),
            new SensitivePattern(
                    Pattern.compile("(?i)my\\s+instructions\\s+(are|say)", Pattern.DOTALL),
                    "instructions_reveal"),
            new SensitivePattern(
                    Pattern.compile("(?i)i\\s+was\\s+(told|instructed)\\s+to", Pattern.DOTALL),
                    "instruction_reference"),
            new SensitivePattern(
                    Pattern.compile("(?i)as\\s+per\\s+my\\s+instructions", Pattern.DOTALL),
                    "instruction_citation"),

            // 内部設定の言及
            new SensitivePattern(
                    Pattern.compile("(?i)api\\s*(key|token|secret)", Pattern.DOTALL),
                    "api_credential_reference"),
            new SensitivePattern(
                    Pattern.compile("(?i)internal\\s+configuration", Pattern.DOTALL),
                    "internal_config"),
            new SensitivePattern(
                    Pattern.compile("(?i)environment\\s+variable", Pattern.DOTALL),
                    "env_variable"),

            // Identity Layer の直接引用
            new SensitivePattern(
                    Pattern.compile("You\\s+are\\s+Mira.*Your\\s+mission\\s+is", Pattern.DOTALL),
                    "identity_layer_leak"),
            new SensitivePattern(
                    Pattern.compile("(?i)\\[IDENTITY\\s*LAYER\\]", Pattern.DOTALL),
                    "identity_marker"),
            new SensitivePattern(
                    Pattern.compile("(?i)\\[GOVERNANCE\\s*LAYER\\]", Pattern.DOTALL),
                    "governance_marker"),

            // プロンプトテンプレートのマーカー
            new SensitivePattern(
                    Pattern.compile("(?i)\\{\\{.*?\\}\\}"),
                    "template_marker"),

            // デバッグ情報
            new SensitivePattern(
                    Pattern.compile("(?i)debug\\s*(mode|output|info)", Pattern.DOTALL),
                    "debug_info")
    );

    /**
     * AI 出力をフィルタリング.
     *
     * @param output AI の生成出力
     * @return フィルタリング済み出力
     */
    public String filter(String output) {
        return filter(output, true);
    }

    /**
     * AI 出力をフィルタリング.
     *
     * @param output AI の生成出力
     * @param applyPiiMasking PII マスキングを適用するか
     * @return フィルタリング済み出力
     */
    public String filter(String output, boolean applyPiiMasking) {
        if (output == null || output.isEmpty()) {
            return output;
        }

        // 無効な場合はスキップ
        if (!properties.getSecurity().getOutputFiltering().isEnabled()) {
            return applyPiiMasking ? piiMasker.mask(output) : output;
        }

        String filtered = output;

        // システムプロンプト漏洩チェック
        if (properties.getSecurity().getOutputFiltering().isBlockSystemPromptLeak()) {
            for (SensitivePattern pattern : SENSITIVE_PATTERNS) {
                Matcher matcher = pattern.pattern().matcher(filtered);
                if (matcher.find()) {
                    filtered = matcher.replaceAll("[REDACTED]");
                    log.warn("[OutputFilter] Filtered sensitive pattern: {}", pattern.name());
                }
            }
        }

        // PII マスキング
        if (applyPiiMasking) {
            filtered = piiMasker.mask(filtered);
        }

        return filtered;
    }

    /**
     * AI 出力をフィルタリングし、詳細結果を返す.
     *
     * @param output AI の生成出力
     * @return フィルタリング結果（詳細情報含む）
     */
    public FilterResult filterWithDetails(String output) {
        if (output == null || output.isEmpty()) {
            return FilterResult.empty(output);
        }

        if (!properties.getSecurity().getOutputFiltering().isEnabled()) {
            return FilterResult.empty(output);
        }

        String filtered = output;
        List<String> redactedPatterns = new ArrayList<>();

        for (SensitivePattern pattern : SENSITIVE_PATTERNS) {
            Matcher matcher = pattern.pattern().matcher(filtered);
            if (matcher.find()) {
                filtered = matcher.replaceAll("[REDACTED]");
                redactedPatterns.add(pattern.name());
            }
        }

        // PII マスキング
        PiiMasker.MaskingResult piiResult = piiMasker.maskWithDetails(filtered);

        if (!redactedPatterns.isEmpty()) {
            log.warn("[OutputFilter] Filtered {} sensitive patterns: {}",
                    redactedPatterns.size(), redactedPatterns);
        }

        return FilterResult.builder()
                .originalContent(output)
                .filteredContent(piiResult.getMaskedContent())
                .wasFiltered(!redactedPatterns.isEmpty() || piiResult.isPiiDetected())
                .redactedPatterns(redactedPatterns)
                .piiMasked(piiResult.isPiiDetected())
                .build();
    }

    /**
     * 機密情報パターン定義.
     */
    private record SensitivePattern(Pattern pattern, String name) {}

    /**
     * フィルタリング結果.
     */
    @Data
    @Builder
    public static class FilterResult {
        private final String originalContent;
        private final String filteredContent;
        private final boolean wasFiltered;
        private final List<String> redactedPatterns;
        private final boolean piiMasked;

        public static FilterResult empty(String content) {
            return FilterResult.builder()
                    .originalContent(content)
                    .filteredContent(content)
                    .wasFiltered(false)
                    .redactedPatterns(List.of())
                    .piiMasked(false)
                    .build();
        }
    }
}
