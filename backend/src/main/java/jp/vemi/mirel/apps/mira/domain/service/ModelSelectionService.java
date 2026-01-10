/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraModelRegistry;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraModelRegistryRepository;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jp.vemi.framework.util.SanitizeUtil;

/**
 * モデル選択サービス.
 * <p>
 * 優先順位に基づいてAIモデルを選択し、利用可能なモデル一覧を提供します。
 * </p>
 * 
 * <p>
 * <b>優先順位:</b> リクエスト指定 &gt; ユーザーコンテキスト &gt; テナント設定 &gt; システム設定 &gt; プロパティファイル
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelSelectionService {

    private final MiraModelRegistryRepository modelRegistryRepository;
    private final MiraSettingService settingService;
    private final MiraAiProperties aiProperties;

    /**
     * 優先順位に基づいてモデルを選択.
     * 
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @param contextId
     *            コンテキストID（任意）
     * @param forceModel
     *            リクエストで指定されたモデル（最優先）
     * @return 選択されたモデル名
     */
    @Transactional(readOnly = true)
    public String resolveModel(String tenantId, String userId, String contextId, String forceModel) {
        // 1. リクエストで明示的に指定されている場合（最優先）
        if (forceModel != null && !forceModel.isEmpty()) {
            log.info("Using force-specified model: {} (tenant={}, user={})", SanitizeUtil.forLog(forceModel),
                    SanitizeUtil.forLog(tenantId), SanitizeUtil.forLog(userId));
            return forceModel;
        }

        // 2. ユーザーコンテキスト設定
        // TODO: Phase 3で実装予定（MiraUserContextテーブルが必要）
        // if (contextId != null && !contextId.isEmpty()) {
        // Optional<MiraUserContext> context =
        // userContextRepository.findByUserIdAndContextId(userId, contextId);
        // if (context.isPresent() && context.get().getPreferredModel() != null) {
        // log.info("Using user context model: {} (tenant={}, user={}, context={})",
        // context.get().getPreferredModel(), tenantId, userId, contextId);
        // return context.get().getPreferredModel();
        // }
        // }

        // 3. テナント設定
        String tenantModel = settingService.getString(tenantId, MiraSettingService.KEY_AI_MODEL, null);
        if (tenantModel != null && !tenantModel.isEmpty()) {
            log.info("Using tenant model: {} (tenant={})", tenantModel, tenantId);
            return tenantModel;
        }

        // 4. システム設定
        String systemModel = settingService.getString(null, MiraSettingService.KEY_AI_MODEL, null);
        if (systemModel != null && !systemModel.isEmpty()) {
            log.info("Using system model: {}", systemModel);
            return systemModel;
        }

        // 5. プロパティファイルのデフォルト（プロバイダに応じた値）
        String provider = settingService.getAiProvider(tenantId);
        String defaultModel = getDefaultModelForProvider(provider);
        log.info("Using default model from properties: {} (provider={})", defaultModel, provider);
        return defaultModel;
    }

    /**
     * プロバイダに応じたデフォルトモデルを取得.
     * 
     * @param provider
     *            プロバイダ名
     * @return デフォルトモデル名
     */
    private String getDefaultModelForProvider(String provider) {
        switch (provider) {
            case MiraAiProperties.PROVIDER_VERTEX_AI_GEMINI:
                return aiProperties.getVertexAi().getModel();
            case MiraAiProperties.PROVIDER_AZURE_OPENAI:
                return aiProperties.getAzureOpenai().getDeploymentName();
            case MiraAiProperties.PROVIDER_GITHUB_MODELS:
                return aiProperties.getGithubModels().getModel();
            default:
                return aiProperties.getGithubModels().getModel();
        }
    }

    /**
     * プロバイダごとの有効なモデル一覧を取得.
     * 
     * @param provider
     *            プロバイダ名
     * @return 有効なモデルリスト
     */
    @Transactional(readOnly = true)
    public List<MiraModelRegistry> getAvailableModels(String provider) {
        return modelRegistryRepository.findByProviderAndIsActiveTrue(provider);
    }

    /**
     * すべての有効なモデル一覧を取得.
     * 
     * @return 有効なモデルリスト
     */
    @Transactional(readOnly = true)
    public List<MiraModelRegistry> getAllAvailableModels() {
        return modelRegistryRepository.findByIsActiveTrue();
    }

    /**
     * モデルの機能をチェック.
     * 
     * @param provider
     *            プロバイダ名
     * @param modelName
     *            モデル名
     * @param capability
     *            機能名（例: "TOOL_CALLING"）
     * @return 機能をサポートしている場合true
     */
    @Transactional(readOnly = true)
    public boolean supportsCapability(String provider, String modelName, String capability) {
        Optional<MiraModelRegistry> model = modelRegistryRepository.findByProviderAndModelName(provider, modelName);
        return model.isPresent() && model.get().getCapabilitiesList().contains(capability);
    }
}
