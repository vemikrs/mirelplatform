/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * AI プロバイダファクトリ.
 * 
 * <p>設定に基づいて適切な AI プロバイダを選択します。</p>
 */
@Slf4j
@Component
public class AiProviderFactory {

    private final Map<String, AiProviderClient> providers;
    private final MiraAiProperties properties;

    public AiProviderFactory(List<AiProviderClient> providerList, MiraAiProperties properties) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AiProviderClient::getProviderName, Function.identity()));
        this.properties = properties;

        log.info("AiProviderFactory initialized with providers: {}", providers.keySet());
    }

    /**
     * 設定されたプロバイダを取得.
     * 
     * @return AI プロバイダクライアント
     */
    public AiProviderClient getProvider() {
        String providerName = properties.getProvider();
        
        // モックが有効な場合はモックを優先
        if (properties.getMock().isEnabled()) {
            log.debug("Mock provider is enabled, using mock");
            return getProvider("mock").orElseThrow(() -> 
                    new IllegalStateException("Mock provider is enabled but not available"));
        }

        return getProvider(providerName).orElseGet(() -> {
            log.warn("Provider '{}' not available, falling back to mock", providerName);
            return getProvider("mock").orElseThrow(() ->
                    new IllegalStateException("No AI provider available"));
        });
    }

    /**
     * 指定されたプロバイダを取得.
     * 
     * @param providerName プロバイダ名
     * @return AI プロバイダクライアント（Optional）
     */
    public Optional<AiProviderClient> getProvider(String providerName) {
        AiProviderClient provider = providers.get(providerName);
        if (provider != null && provider.isAvailable()) {
            return Optional.of(provider);
        }
        return Optional.empty();
    }

    /**
     * 利用可能なプロバイダ一覧を取得.
     * 
     * @return プロバイダ名のリスト
     */
    public List<String> getAvailableProviders() {
        return providers.values().stream()
                .filter(AiProviderClient::isAvailable)
                .map(AiProviderClient::getProviderName)
                .collect(Collectors.toList());
    }

    /**
     * チャット応答を生成（フォールバック付き）.
     * 
     * @param request AI リクエスト
     * @return AI 応答
     */
    public AiResponse chat(AiRequest request) {
        AiProviderClient provider = getProvider();
        log.debug("Using provider: {}", provider.getProviderName());
        
        AiResponse response = provider.chat(request);
        
        // エラー時にフォールバック
        if (response.hasError() && !provider.getProviderName().equals("mock")) {
            log.warn("Primary provider failed, trying fallback to mock");
            Optional<AiProviderClient> fallback = getProvider("mock");
            if (fallback.isPresent()) {
                return fallback.get().chat(request);
            }
        }
        
        return response;
    }
}
