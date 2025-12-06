/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PII（個人情報）マスカー.
 * <p>
 * テキスト内の個人情報を検出し、マスキングする。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PiiMasker {

    private final MiraAiProperties properties;

    /**
     * PII パターン定義.
     */
    private static final Map<String, Pattern> PII_PATTERNS = new LinkedHashMap<>();

    static {
        // メールアドレス
        PII_PATTERNS.put("email",
                Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));

        // 電話番号（日本）
        PII_PATTERNS.put("phone",
                Pattern.compile("0[0-9]{1,4}[- ]?[0-9]{1,4}[- ]?[0-9]{4}"));

        // 携帯電話（日本）
        PII_PATTERNS.put("mobile",
                Pattern.compile("0[7-9]0[- ]?[0-9]{4}[- ]?[0-9]{4}"));

        // クレジットカード番号（16桁）
        PII_PATTERNS.put("credit_card",
                Pattern.compile("\\b(?:\\d{4}[- ]?){3}\\d{4}\\b"));

        // マイナンバー（12桁）
        PII_PATTERNS.put("my_number",
                Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"));

        // 郵便番号
        PII_PATTERNS.put("postal_code",
                Pattern.compile("〒?\\d{3}[- ]?\\d{4}"));

        // IPアドレス（IPv4）
        PII_PATTERNS.put("ipv4",
                Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"));

        // 日本の住所パターン（都道府県から始まる）
        PII_PATTERNS.put("address_jp",
                Pattern.compile("(東京都|北海道|(?:京都|大阪)府|.{2,3}県).{1,10}[市区町村].{1,20}"));
    }

    /**
     * テキスト内の PII をマスキング.
     *
     * @param content マスキング対象テキスト
     * @return マスキング済みテキスト
     */
    public String mask(String content) {
        return mask(content, false);
    }

    /**
     * テキスト内の PII をマスキング.
     *
     * @param content マスキング対象テキスト
     * @param forLogging ログ用フォーマットを使用するか
     * @return マスキング済みテキスト
     */
    public String mask(String content, boolean forLogging) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // 無効な場合はスキップ
        if (!properties.getSecurity().getPiiMasking().isEnabled()) {
            return content;
        }

        String masked = content;
        List<String> enabledPatterns = properties.getSecurity().getPiiMasking().getPatterns();

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            // 有効なパターンのみ適用
            if (!enabledPatterns.isEmpty() && !enabledPatterns.contains(entry.getKey())) {
                continue;
            }

            String maskFormat = forLogging ? "[%s:MASKED]" : "***";
            String replacement = forLogging
                    ? String.format(maskFormat, entry.getKey().toUpperCase())
                    : "***";
            masked = entry.getValue().matcher(masked).replaceAll(replacement);
        }

        return masked;
    }

    /**
     * PII を検出して詳細情報を返す.
     *
     * @param content 検査対象テキスト
     * @return マスキング結果（詳細情報含む）
     */
    public MaskingResult maskWithDetails(String content) {
        if (content == null || content.isEmpty()) {
            return MaskingResult.empty(content);
        }

        if (!properties.getSecurity().getPiiMasking().isEnabled()) {
            return MaskingResult.empty(content);
        }

        List<PiiMatch> matches = new ArrayList<>();
        String masked = content;
        List<String> enabledPatterns = properties.getSecurity().getPiiMasking().getPatterns();

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            if (!enabledPatterns.isEmpty() && !enabledPatterns.contains(entry.getKey())) {
                continue;
            }

            Matcher matcher = entry.getValue().matcher(content);
            while (matcher.find()) {
                matches.add(PiiMatch.builder()
                        .type(entry.getKey())
                        .position(matcher.start())
                        .length(matcher.end() - matcher.start())
                        .build());

                if (log.isDebugEnabled()) {
                    log.debug("[PiiMasker] Detected PII: type={}, position={}",
                            entry.getKey(), matcher.start());
                }
            }
            masked = entry.getValue().matcher(masked).replaceAll("***");
        }

        if (!matches.isEmpty()) {
            log.info("[PiiMasker] Masked {} PII instances: types={}",
                    matches.size(),
                    matches.stream().map(PiiMatch::getType).distinct().toList());
        }

        return MaskingResult.builder()
                .originalContent(content)
                .maskedContent(masked)
                .piiDetected(!matches.isEmpty())
                .matches(matches)
                .build();
    }

    /**
     * 特定の PII タイプのみをマスキング.
     *
     * @param content マスキング対象テキスト
     * @param piiTypes マスキング対象の PII タイプ
     * @return マスキング済みテキスト
     */
    public String maskSpecificTypes(String content, List<String> piiTypes) {
        if (content == null || content.isEmpty() || piiTypes == null || piiTypes.isEmpty()) {
            return content;
        }

        String masked = content;
        for (String type : piiTypes) {
            Pattern pattern = PII_PATTERNS.get(type);
            if (pattern != null) {
                masked = pattern.matcher(masked).replaceAll("***");
            }
        }
        return masked;
    }

    /**
     * PII 検出結果.
     */
    @Data
    @Builder
    public static class MaskingResult {
        private final String originalContent;
        private final String maskedContent;
        private final boolean piiDetected;
        private final List<PiiMatch> matches;

        public static MaskingResult empty(String content) {
            return MaskingResult.builder()
                    .originalContent(content)
                    .maskedContent(content)
                    .piiDetected(false)
                    .matches(List.of())
                    .build();
        }
    }

    /**
     * PII マッチ情報.
     */
    @Data
    @Builder
    public static class PiiMatch {
        private final String type;
        private final int position;
        private final int length;
    }
}
