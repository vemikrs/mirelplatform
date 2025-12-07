/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

/**
 * AI プロバイダクライアントインタフェース.
 * 
 * <p>Azure OpenAI / OpenAI / Mock などのプロバイダを抽象化します。</p>
 */
public interface AiProviderClient {

    /**
     * チャット応答を生成.
     * 
     * @param request AI リクエスト
     * @return AI 応答
     */
    AiResponse chat(AiRequest request);

    /**
     * プロバイダが利用可能かどうか.
     * 
     * @return 利用可能な場合 true
     */
    boolean isAvailable();

    /**
     * プロバイダ名を取得.
     * 
     * @return プロバイダ名
     */
    String getProviderName();
}
