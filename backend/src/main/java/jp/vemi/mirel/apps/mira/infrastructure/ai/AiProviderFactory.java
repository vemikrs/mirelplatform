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

import jp.vemi.mirel.apps.mira.domain.service.MiraSettingService;
import jp.vemi.mirel.apps.mira.domain.service.TokenQuotaService;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
import lombok.extern.slf4j.Slf4j;

/**
 * AI プロバイダファクトリ.
 * 
 * <p>
 * 設定に基づいて適切な AI プロバイダを選択します。
 * </p>
 */
@Slf4j
@Component
public class AiProviderFactory {

    private final Map<String, AiProviderClient> providers;
    private final MiraAiProperties properties;
    private final MiraSettingService settingService;
    private final MiraMetrics metrics;
    private final TokenQuotaService tokenQuotaService;

    public AiProviderFactory(
            List<AiProviderClient> providerList,
            MiraAiProperties properties,
            MiraSettingService settingService,
            MiraMetrics metrics,
            TokenQuotaService tokenQuotaService) {

        this.providers = providerList.stream()
                .collect(Collectors.toMap(AiProviderClient::getProviderName, Function.identity()));
        this.properties = properties;
        this.settingService = settingService;
        this.metrics = metrics;
        this.tokenQuotaService = tokenQuotaService;

        log.info("AiProviderFactory initialized with providers: {}", providers.keySet());
    }

    /**
     * テナント用AIクライアントを作成（メトリクス計測付き）.
     *
     * @param tenantId
     *            テナントID
     * @return メトリクス計測をラップしたAIクライアント
     */
    public AiProviderClient createClient(String tenantId) {
        String providerName = settingService.getAiProvider(tenantId);

        log.info("Selecting AI provider: '{}' for tenant: '{}'", providerName, tenantId);

        AiProviderClient baseClient = getProvider(providerName)
                .orElseThrow(() -> {
                    log.error("Requested provider '{}' for tenant '{}' is not available.", providerName, tenantId);
                    return new IllegalStateException("Requested provider '" + providerName + "' is not available.");
                });

        // メトリクス計測とトークン使用量記録をラップ
        return new MetricsWrappedAiClient(baseClient, metrics, tokenQuotaService, tenantId);
    }

    /**
     * 設定されたプロバイダを取得.
     * 
     * @return AI プロバイダクライアント
     */
    public AiProviderClient getProvider() {
        // Legacy method support if needed, or redirect to default tenant
        // For backward compatibility or internal use without tenant context
        String providerName = properties.getProvider();

        // モックが有効な場合はモックを優先するロジックを削除し、純粋に provider 設定値に従うように変更
        // if (properties.getMock().isEnabled()) {
        // return getProvider("mock")
        // .orElseThrow(() -> new IllegalStateException("Mock provider is enabled but
        // not available"));
        // }

        return getProvider(providerName).orElseThrow(() -> new IllegalStateException(
                "Requested provider '" + providerName + "' is not available. Please check your configuration."));
    }

    /**
     * 指定されたプロバイダを取得.
     * 
     * @param providerName
     *            プロバイダ名
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
     * @param request
     *            AI リクエスト
     * @return AI 応答
     */
    public AiResponse chat(AiRequest request) {
        // Note: This method seems to assume a default provider context.
        // If possible, we should pass tenantId here.
        // Assuming 'request' object might have tenant context or we default.
        AiProviderClient provider = getProvider();
        log.debug("Using provider: {}", provider.getProviderName());

        AiResponse response = provider.chat(request);

        // エラー時にフォールバックしない（厳格モード）
        if (response.hasError()) {
            log.error("Provider '{}' returned error: {}", provider.getProviderName(), response.getMetadata());
            // 必要に応じて例外をスローするか、エラーレスポンスをそのまま返す
        }

        return response;
    }
}
