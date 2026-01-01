/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraSystemSetting;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraTenantSetting;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraSystemSettingRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraTenantSettingRepository;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira 設定サービス.
 * <p>
 * システム設定およびテナント設定を管理し、プロパティファイルの設定とマージして提供します。
 * 優先順位: テナント設定 > システム設定 > application.yml (MiraAiProperties)
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiraSettingService {

    private final MiraSystemSettingRepository systemSettingRepository;
    private final MiraTenantSettingRepository tenantSettingRepository;
    private final MiraAiProperties miraAiProperties;

    // Keys
    public static final String KEY_AI_PROVIDER = "ai.provider";
    public static final String KEY_AI_MODEL = "ai.model";
    public static final String KEY_AI_TEMPERATURE = "ai.temperature";
    public static final String KEY_AI_MAX_TOKENS = "ai.max_tokens";
    public static final String KEY_LIMIT_RPM = "limit.rpm"; // Requests Per Minute
    public static final String KEY_LIMIT_RPH = "limit.rph"; // Requests Per Hour
    public static final String KEY_LIMIT_DAILY_QUOTA = "limit.daily_quota";
    public static final String KEY_TAVILY_API_KEY = "tavily.api_key";
    public static final String KEY_VECTOR_SEARCH_THRESHOLD = "vector.search.threshold";
    public static final String KEY_VECTOR_SEARCH_TOP_K = "vector.search.top_k";

    // RAG拡張設定キー (Phase 4 & 5)
    public static final String KEY_RAG_QUESTION_GENERATION_ENABLED = "rag.question_generation.enabled";
    public static final String KEY_RAG_QUESTION_GENERATION_COUNT = "rag.question_generation.count";
    public static final String KEY_RAG_STRUCTURED_PARSING_EXCEL_ENABLED = "rag.structured_parsing.excel.enabled";
    public static final String KEY_RAG_STRUCTURED_PARSING_CSV_ENABLED = "rag.structured_parsing.csv.enabled";
    public static final String KEY_RAG_STRUCTURED_PARSING_PDF_ENABLED = "rag.structured_parsing.pdf.enabled";
    public static final String KEY_RAG_EXCEL_MAX_SIZE_MB = "rag.excel.max_size_mb";

    /**
     * 有効な設定値を取得します（String）.
     *
     * @param tenantId
     *            テナントID
     * @param key
     *            設定キー
     * @param devaultValue
     *            デフォルト値
     * @return 設定値
     */
    public String getString(String tenantId, String key, String devaultValue) {
        // 1. Check Tenant Setting
        if (tenantId != null) {
            Optional<MiraTenantSetting> tenantSetting = tenantSettingRepository
                    .findById(new MiraTenantSetting.PK(tenantId, key));
            if (tenantSetting.isPresent()) {
                return tenantSetting.get().getValue();
            }
        }

        // 2. Check System Setting
        Optional<MiraSystemSetting> systemSetting = systemSettingRepository.findById(key);
        if (systemSetting.isPresent()) {
            return systemSetting.get().getValue();
        }

        // 3. Return Default
        return devaultValue;
    }

    /**
     * 有効な設定値を取得します（Integer）.
     */
    public Integer getInteger(String tenantId, String key, Integer defaultValue) {
        String val = getString(tenantId, key, null);
        if (val == null)
            return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer format for key {}: {}", key, val);
            return defaultValue;
        }
    }

    /**
     * 有効な設定値を取得します（Double）.
     */
    public Double getDouble(String tenantId, String key, Double defaultValue) {
        String val = getString(tenantId, key, null);
        if (val == null)
            return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid double format for key {}: {}", key, val);
            return defaultValue;
        }
    }

    /**
     * 有効な設定値を取得します（Long）.
     */
    public Long getLong(String tenantId, String key, Long defaultValue) {
        String val = getString(tenantId, key, null);
        if (val == null)
            return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid long format for key {}: {}", key, val);
            return defaultValue;
        }
    }

    // ===================================================================================
    // Specific Getters
    // ===================================================================================

    public String getAiProvider(String tenantId) {
        return getString(tenantId, KEY_AI_PROVIDER, miraAiProperties.getProvider());
    }

    public String getAiModel(String tenantId) {
        // プロバイダに応じたデフォルトモデルを取得
        String provider = getAiProvider(tenantId);
        String defaultModel;

        switch (provider) {
            case MiraAiProperties.PROVIDER_VERTEX_AI_GEMINI:
                defaultModel = miraAiProperties.getVertexAi().getModel();
                break;
            case MiraAiProperties.PROVIDER_AZURE_OPENAI:
                defaultModel = miraAiProperties.getAzureOpenai().getDeploymentName();
                break;
            case MiraAiProperties.PROVIDER_GITHUB_MODELS:
                defaultModel = miraAiProperties.getGithubModels().getModel();
                break;
            default:
                log.warn("Unknown provider: {}, falling back to github-models default", provider);
                defaultModel = miraAiProperties.getGithubModels().getModel();
        }

        return getString(tenantId, KEY_AI_MODEL, defaultModel);
    }

    public Double getAiTemperature(String tenantId) {
        // プロバイダに応じたデフォルト temperature を取得
        String provider = getAiProvider(tenantId);
        Double defaultTemp;

        switch (provider) {
            case MiraAiProperties.PROVIDER_VERTEX_AI_GEMINI:
                defaultTemp = miraAiProperties.getVertexAi().getTemperature();
                break;
            case MiraAiProperties.PROVIDER_AZURE_OPENAI:
                defaultTemp = miraAiProperties.getAzureOpenai().getTemperature();
                break;
            case MiraAiProperties.PROVIDER_GITHUB_MODELS:
                defaultTemp = miraAiProperties.getGithubModels().getTemperature();
                break;
            default:
                defaultTemp = 0.7;
        }

        return getDouble(tenantId, KEY_AI_TEMPERATURE, defaultTemp);
    }

    public Integer getAiMaxTokens(String tenantId) {
        // プロバイダに応じたデフォルト maxTokens を取得
        String provider = getAiProvider(tenantId);
        Integer defaultMax;

        switch (provider) {
            case MiraAiProperties.PROVIDER_VERTEX_AI_GEMINI:
                defaultMax = 4096; // Vertex AI のデフォルト（プロパティに未定義の場合）
                break;
            case MiraAiProperties.PROVIDER_AZURE_OPENAI:
                defaultMax = miraAiProperties.getAzureOpenai().getMaxTokens();
                break;
            case MiraAiProperties.PROVIDER_GITHUB_MODELS:
                defaultMax = miraAiProperties.getGithubModels().getMaxTokens();
                break;
            default:
                defaultMax = 4096;
        }

        return getInteger(tenantId, KEY_AI_MAX_TOKENS, defaultMax);
    }

    public int getRateLimitRpm(String tenantId) {
        return getInteger(tenantId, KEY_LIMIT_RPM, miraAiProperties.getRateLimit().getRequestsPerMinute());
    }

    public int getRateLimitRph(String tenantId) {
        // Default to a reasonably high number if not configured in properties (as
        // property doesn't exist yet)
        return getInteger(tenantId, KEY_LIMIT_RPH, 1000);
    }

    public long getDailyTokenQuota(String tenantId) {
        return getLong(tenantId, KEY_LIMIT_DAILY_QUOTA, miraAiProperties.getQuota().getDailyTokenLimit());
    }

    public double getVectorSearchThreshold(String tenantId) {
        return getDouble(tenantId, KEY_VECTOR_SEARCH_THRESHOLD, miraAiProperties.getVector().getSearchThreshold());
    }

    public int getVectorSearchTopK(String tenantId) {
        // Default to 5
        return getInteger(tenantId, KEY_VECTOR_SEARCH_TOP_K, 5);
    }

    // ===================================================================================
    // RAG拡張設定 Getters
    // ===================================================================================

    /**
     * 想定質問生成が有効かどうか.
     */
    public boolean isQuestionGenerationEnabled(String tenantId) {
        return "true".equalsIgnoreCase(
                getString(tenantId, KEY_RAG_QUESTION_GENERATION_ENABLED, "false"));
    }

    /**
     * 生成する想定質問の数.
     */
    public int getQuestionGenerationCount(String tenantId) {
        return getInteger(tenantId, KEY_RAG_QUESTION_GENERATION_COUNT, 3);
    }

    /**
     * Excel構造解析が有効かどうか.
     */
    public boolean isExcelStructuredParsingEnabled(String tenantId) {
        return "true".equalsIgnoreCase(
                getString(tenantId, KEY_RAG_STRUCTURED_PARSING_EXCEL_ENABLED, "true"));
    }

    /**
     * CSV構造解析が有効かどうか.
     */
    public boolean isCsvStructuredParsingEnabled(String tenantId) {
        return "true".equalsIgnoreCase(
                getString(tenantId, KEY_RAG_STRUCTURED_PARSING_CSV_ENABLED, "true"));
    }

    /**
     * PDF表構造解析が有効かどうか.
     */
    public boolean isPdfStructuredParsingEnabled(String tenantId) {
        return "true".equalsIgnoreCase(
                getString(tenantId, KEY_RAG_STRUCTURED_PARSING_PDF_ENABLED, "false"));
    }

    /**
     * Excel処理のファイルサイズ上限（MB）.
     */
    public int getExcelMaxSizeMb(String tenantId) {
        return getInteger(tenantId, KEY_RAG_EXCEL_MAX_SIZE_MB, 10);
    }

    // ===================================================================================
    // リランカー設定 Getters
    // ===================================================================================

    /** リランカー設定キー. */
    public static final String KEY_RERANKER_ENABLED = "reranker.enabled";
    public static final String KEY_RERANKER_PROVIDER = "reranker.provider";
    public static final String KEY_RERANKER_MODEL = "reranker.model";
    public static final String KEY_RERANKER_TOP_N = "reranker.top_n";
    public static final String KEY_RERANKER_MIN_CANDIDATES = "reranker.min_candidates";
    public static final String KEY_RERANKER_TIMEOUT_MS = "reranker.timeout_ms";

    /**
     * リランカーが有効かどうか（レイヤード設定対応）.
     */
    public boolean isRerankerEnabled(String tenantId) {
        String value = getString(tenantId, KEY_RERANKER_ENABLED, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return miraAiProperties.getReranker().isEnabled();
    }

    /**
     * リランカープロバイダー（レイヤード設定対応）.
     */
    public String getRerankerProvider(String tenantId) {
        return getString(tenantId, KEY_RERANKER_PROVIDER,
                miraAiProperties.getReranker().getProvider());
    }

    /**
     * リランカーモデル名（レイヤード設定対応）.
     */
    public String getRerankerModel(String tenantId) {
        return getString(tenantId, KEY_RERANKER_MODEL,
                miraAiProperties.getReranker().getModel());
    }

    /**
     * リランカーのtopN（レイヤード設定対応）.
     */
    public int getRerankerTopN(String tenantId) {
        return getInteger(tenantId, KEY_RERANKER_TOP_N,
                miraAiProperties.getReranker().getTopN());
    }

    /**
     * リランキング実行の候補数閾値（レイヤード設定対応）.
     */
    public int getRerankerMinCandidates(String tenantId) {
        return getInteger(tenantId, KEY_RERANKER_MIN_CANDIDATES,
                miraAiProperties.getReranker().getMinCandidates());
    }

    /**
     * リランカータイムアウト（ミリ秒、レイヤード設定対応）.
     */
    public int getRerankerTimeoutMs(String tenantId) {
        return getInteger(tenantId, KEY_RERANKER_TIMEOUT_MS,
                miraAiProperties.getReranker().getTimeoutMs());
    }

    // ===================================================================================
    // Setters
    // ===================================================================================

    @Transactional
    public void saveSystemSetting(String key, String value) {
        MiraSystemSetting setting = systemSettingRepository.findById(key)
                .orElse(MiraSystemSetting.builder().key(key).build());
        setting.setValue(value);
        systemSettingRepository.save(setting);
    }

    @Transactional
    public void saveTenantSetting(String tenantId, String key, String value) {
        MiraTenantSetting setting = tenantSettingRepository.findById(new MiraTenantSetting.PK(tenantId, key))
                .orElse(MiraTenantSetting.builder().tenantId(tenantId).key(key).build());
        setting.setValue(value);
        tenantSettingRepository.save(setting);
    }

    @Transactional
    public void deleteTenantSetting(String tenantId, String key) {
        tenantSettingRepository.deleteById(new MiraTenantSetting.PK(tenantId, key));
    }
}
