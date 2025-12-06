/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * モック AI クライアント.
 * 
 * <p>テスト・CI/CD 環境で外部 API に依存せずに動作確認を行うためのモッククライアント。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockAiClient implements AiProviderClient {

    private final MiraAiProperties properties;

    private static final String PROVIDER_NAME = "mock";

    @Override
    public AiResponse chat(AiRequest request) {
        log.debug("MockAiClient.chat() called with request: {}", request);

        long startTime = System.currentTimeMillis();

        // 応答遅延シミュレーション
        simulateDelay();

        // パターンマッチ応答を検索
        String response = findMatchingResponse(request.getUserPrompt());

        long latencyMs = System.currentTimeMillis() - startTime;

        return AiResponse.success(
                response,
                AiResponse.Metadata.builder()
                        .model("mock-model")
                        .finishReason("stop")
                        .promptTokens(estimateTokens(request.getUserPrompt()))
                        .completionTokens(estimateTokens(response))
                        .totalTokens(estimateTokens(request.getUserPrompt()) + estimateTokens(response))
                        .latencyMs(latencyMs)
                        .build()
        );
    }

    @Override
    public boolean isAvailable() {
        return properties.getMock().isEnabled();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * パターンマッチ応答を検索.
     */
    private String findMatchingResponse(String userPrompt) {
        if (userPrompt == null) {
            return properties.getMock().getDefaultResponse();
        }

        Map<String, String> responses = properties.getMock().getResponses();
        if (responses != null && !responses.isEmpty()) {
            for (Map.Entry<String, String> entry : responses.entrySet()) {
                String pattern = entry.getKey();
                if (matchesPattern(userPrompt, pattern)) {
                    log.debug("Matched pattern: {} -> {}", pattern, entry.getValue());
                    return entry.getValue();
                }
            }
        }

        // モード別デフォルト応答
        return generateContextualResponse(userPrompt);
    }

    /**
     * パターンマッチ判定.
     */
    private boolean matchesPattern(String text, String pattern) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                    .matcher(text)
                    .find();
        } catch (Exception e) {
            // 正規表現エラー時は部分一致で判定
            return text.toLowerCase().contains(pattern.toLowerCase());
        }
    }

    /**
     * コンテキストに応じたデフォルト応答を生成.
     */
    private String generateContextualResponse(String userPrompt) {
        String lowerPrompt = userPrompt.toLowerCase();

        if (lowerPrompt.contains("この画面") || lowerPrompt.contains("説明")) {
            return "この画面では、選択したアイテムの編集や設定を行うことができます。\n\n" +
                   "**主な機能:**\n" +
                   "- 項目の追加・編集・削除\n" +
                   "- 設定の保存\n" +
                   "- プレビュー表示\n\n" +
                   "詳しい操作方法については、ヘルプボタンをクリックしてください。";
        }

        if (lowerPrompt.contains("エラー") || lowerPrompt.contains("問題")) {
            return "エラーの原因として以下が考えられます:\n\n" +
                   "1. **入力値の形式が不正** - 必須項目が未入力か、形式が正しくない可能性があります\n" +
                   "2. **権限不足** - この操作に必要な権限がない可能性があります\n" +
                   "3. **一時的なサーバーエラー** - しばらく待ってから再試行してください\n\n" +
                   "**推奨アクション:**\n" +
                   "- 入力内容を確認してください\n" +
                   "- ページを再読み込みしてみてください\n" +
                   "- 問題が続く場合は管理者にお問い合わせください";
        }

        if (lowerPrompt.contains("設定") || lowerPrompt.contains("手順")) {
            return "設定手順は以下の通りです:\n\n" +
                   "1. **設定画面を開く** - 左メニューから「設定」を選択\n" +
                   "2. **項目を選択** - 変更したい設定項目をクリック\n" +
                   "3. **値を入力** - 必要な値を入力してください\n" +
                   "4. **保存** - 「保存」ボタンをクリック\n\n" +
                   "設定が反映されるまで数秒かかる場合があります。";
        }

        // デフォルト応答
        return properties.getMock().getDefaultResponse();
    }

    /**
     * 応答遅延シミュレーション.
     */
    private void simulateDelay() {
        Integer delayMs = properties.getMock().getResponseDelayMs();
        if (delayMs != null && delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * トークン数を概算.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 日本語の場合は文字数の約1.5倍、英語の場合は単語数の約1.3倍として概算
        // ここでは簡易的に文字数 / 3 として計算
        return Math.max(1, text.length() / 3);
    }
}
