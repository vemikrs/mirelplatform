/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jp.vemi.framework.security.PatternRegistry;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * プロンプトインジェクション検出器.
 * <p>
 * ユーザー入力に含まれるプロンプトインジェクションの試みを検出する。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptInjectionDetector {

    private final MiraAiProperties properties;

    /**
     * プロンプトインジェクションパターン.
     */
    private static final List<InjectionPattern> INJECTION_PATTERNS = List.of(
            // 直接的なプロンプト改変
            new InjectionPattern(
                    PatternRegistry.safe(
                            "(?i)(?:ignore|disregard|forget)[^\\n]{0,50}(?:previous|above|prior)[^\\n]{0,50}(?:instructions?|prompt)"),
                    "direct_override", 2),
            new InjectionPattern(
                    Pattern.compile("(?i)system\\s*prompt", Pattern.DOTALL),
                    "system_prompt_reference", 1),
            new InjectionPattern(
                    Pattern.compile("(?i)you\\s+are\\s+(now|actually)", Pattern.DOTALL),
                    "identity_change", 2),
            new InjectionPattern(
                    Pattern.compile("(?i)new\\s+instructions?:", Pattern.DOTALL),
                    "new_instructions", 2),

            // ロール変更の試み
            new InjectionPattern(
                    Pattern.compile("(?i)pretend\\s+(to\\s+be|you\\s+are)", Pattern.DOTALL),
                    "role_change_pretend", 2),
            new InjectionPattern(
                    Pattern.compile("(?i)act\\s+as\\s+(if|a)", Pattern.DOTALL),
                    "role_change_act", 1),
            new InjectionPattern(
                    Pattern.compile("(?i)roleplay\\s+as", Pattern.DOTALL),
                    "role_change_roleplay", 1),

            // プロンプト抽出の試み
            new InjectionPattern(
                    PatternRegistry.safe("(?i)repeat\\s+(?:your|the)[^\\n]{0,50}(?:prompt|instructions)"),
                    "prompt_extraction_repeat", 2),
            new InjectionPattern(
                    Pattern.compile("(?i)what\\s+(are|is)\\s+your\\s+(system\\s+)?prompt", Pattern.DOTALL),
                    "prompt_extraction_what", 2),
            new InjectionPattern(
                    Pattern.compile("(?i)show\\s+me\\s+(your|the)\\s+(system\\s+)?prompt", Pattern.DOTALL),
                    "prompt_extraction_show", 2),
            new InjectionPattern(
                    PatternRegistry.safe("(?i)print\\s+(?:your|the)[^\\n]{0,50}(?:prompt|instructions)"),
                    "prompt_extraction_print", 2),

            // コード実行の試み
            new InjectionPattern(
                    Pattern.compile("(?i)(?:execute|eval|run)\\s+this\\s+code", Pattern.DOTALL),
                    "code_execution", 2),

            // 特殊トークンの挿入
            new InjectionPattern(
                    PatternRegistry.safe("<\\|[^|]{0,100}\\|>"),
                    "special_token_angle", 3),
            new InjectionPattern(
                    Pattern.compile("\\[INST\\]|\\[/INST\\]"),
                    "special_token_inst", 3),
            new InjectionPattern(
                    Pattern.compile("<<SYS>>|<</SYS>>"),
                    "special_token_sys", 3),

            // 制限解除の試み
            new InjectionPattern(
                    PatternRegistry.safe("(?i)(?:bypass|override|disable)[^\\n]{0,50}(?:filter|restriction|safety)"),
                    "bypass_attempt", 2),
            new InjectionPattern(
                    Pattern.compile("(?i)jailbreak|DAN|developer\\s*mode", Pattern.DOTALL),
                    "jailbreak_attempt", 3),

            // 意味タグインジェクション検出（LLM制御タグ）
            new InjectionPattern(
                    Pattern.compile("<\\/?\\s*(system|assistant|user|instruction|prompt|context|role|ai)\\s*>",
                            Pattern.CASE_INSENSITIVE),
                    "semantic_tag_injection", 3),

            // サンドボックスブレイク試行
            new InjectionPattern(
                    Pattern.compile("<\\/\\s*user_?input\\s*>",
                            Pattern.CASE_INSENSITIVE),
                    "sandbox_break_attempt", 3),

            // XML構造インジェクション（HIGH感度時のみ有効）
            new InjectionPattern(
                    Pattern.compile("<\\s*\\w+\\s*>.*?<\\/\\s*\\w+\\s*>",
                            Pattern.DOTALL),
                    "xml_structure_injection", 2));

    /**
     * パターンマッチングの最大入力長.
     * ReDoS攻撃を防ぐため、この長さを超える入力はトランケートされる。
     */
    private static final int MAX_INPUT_LENGTH = 10000;

    /**
     * 入力をチェックしてインジェクションの可能性を検出.
     *
     * @param input
     *            ユーザー入力
     * @return 検出結果
     */
    public InjectionCheckResult check(String input) {
        if (input == null || input.isEmpty()) {
            return InjectionCheckResult.safe();
        }

        // 無効な場合はスキップ
        if (!properties.getSecurity().getPromptInjection().isEnabled()) {
            if (log.isTraceEnabled()) {
                log.trace("[PromptInjectionDetector] Injection detection disabled");
            }
            return InjectionCheckResult.safe();
        }

        // ReDoS対策: 入力長を制限
        String safeInput = input.length() > MAX_INPUT_LENGTH
                ? input.substring(0, MAX_INPUT_LENGTH)
                : input;

        int totalScore = 0;
        List<String> detectedPatterns = new ArrayList<>();

        for (InjectionPattern pattern : INJECTION_PATTERNS) {
            // lgtm[java/polynomial-redos] - input length is bounded by MAX_INPUT_LENGTH
            // (line 145-147), preventing ReDoS
            if (pattern.pattern().matcher(safeInput).find()) {
                totalScore += pattern.weight();
                detectedPatterns.add(pattern.name());

                if (log.isDebugEnabled()) {
                    log.debug("[PromptInjectionDetector] Detected pattern: {} (weight={})",
                            pattern.name(), pattern.weight());
                }
            }
        }

        int softThreshold = properties.getSecurity().getPromptInjection().getSoftBlockThreshold();
        int hardThreshold = properties.getSecurity().getPromptInjection().getHardBlockThreshold();

        boolean softBlock = totalScore >= softThreshold && totalScore < hardThreshold;
        boolean hardBlock = totalScore >= hardThreshold;

        if (hardBlock) {
            log.warn("[PromptInjectionDetector] Hard block triggered: score={}, patterns={}",
                    totalScore, detectedPatterns);
        } else if (softBlock) {
            log.warn("[PromptInjectionDetector] Soft block triggered: score={}, patterns={}",
                    totalScore, detectedPatterns);
        }

        return InjectionCheckResult.builder()
                .suspicious(softBlock || hardBlock)
                .softBlock(softBlock)
                .hardBlock(hardBlock)
                .score(totalScore)
                .detectedPatterns(detectedPatterns)
                .build();
    }

    /**
     * インジェクションパターン定義.
     */
    private record InjectionPattern(Pattern pattern, String name, int weight) {
    }

    /**
     * インジェクション検出結果.
     */
    @Data
    @Builder
    public static class InjectionCheckResult {
        private final boolean suspicious;
        private final boolean softBlock;
        private final boolean hardBlock;
        private final int score;
        private final List<String> detectedPatterns;

        public static InjectionCheckResult safe() {
            return InjectionCheckResult.builder()
                    .suspicious(false)
                    .softBlock(false)
                    .hardBlock(false)
                    .score(0)
                    .detectedPatterns(List.of())
                    .build();
        }
    }
}
