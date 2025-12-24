/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraTokenUsage;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraTokenUsageRepository;
import jp.vemi.mirel.apps.mira.domain.service.MiraContextLayerService;

import jp.vemi.mirel.apps.mira.domain.service.MiraDataExportService; // Import export service
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Mira 管理者用コントローラー.
 */
@RestController
@RequestMapping("apps/mira/api/admin")
@RequiredArgsConstructor
@Tag(name = "Mira Admin", description = "Mira 管理者用API")
@PreAuthorize("hasRole('ADMIN')")
public class MiraAdminController {

    private final MiraContextLayerService contextLayerService;
    private final MiraTokenUsageRepository tokenUsageRepository;
    private final MiraDataExportService dataExportService;
    private final MiraAiProperties properties;
    private final jp.vemi.mirel.apps.mira.domain.service.MiraSettingService settingService; // Inject SettingService
    private final jp.vemi.mirel.apps.mira.domain.service.ModelSelectionService modelSelectionService;
    private final jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory aiProviderFactory;
    private final jp.vemi.mirel.apps.mira.domain.service.MiraKnowledgeBaseService knowledgeBaseService;

    // ==========================================
    // Context Management
    // ==========================================

    @GetMapping("/context/system")
    @Operation(summary = "システムコンテキスト一覧取得")
    public ResponseEntity<List<MiraContextLayer>> getSystemContexts() {
        return ResponseEntity.ok(contextLayerService.getSystemContexts());
    }

    @PostMapping("/context/system")
    @Operation(summary = "システムコンテキスト保存/更新")
    public ResponseEntity<MiraContextLayer> saveSystemContext(@RequestBody MiraContextLayer layer) {
        // 強制的にSYSTEMスコープ
        layer.setScope(MiraContextLayer.ContextScope.SYSTEM);
        return ResponseEntity.ok(contextLayerService.saveContext(layer));
    }

    @GetMapping("/context/tenant/{tenantId}")
    @Operation(summary = "テナントコンテキスト一覧取得")
    public ResponseEntity<List<MiraContextLayer>> getTenantContexts(@PathVariable String tenantId) {
        return ResponseEntity.ok(contextLayerService.getTenantContexts(tenantId));
    }

    @PostMapping("/context/tenant/{tenantId}")
    @Operation(summary = "テナントコンテキスト保存/更新")
    public ResponseEntity<MiraContextLayer> saveTenantContext(
            @PathVariable String tenantId,
            @RequestBody MiraContextLayer layer) {
        // 強制的にTENANTスコープとID
        layer.setScope(MiraContextLayer.ContextScope.TENANT);
        layer.setScopeId(tenantId);
        return ResponseEntity.ok(contextLayerService.saveContext(layer));
    }

    @DeleteMapping("/context/{contextId}")
    @Operation(summary = "コンテキスト削除")
    public ResponseEntity<Void> deleteContext(@PathVariable String contextId) {
        contextLayerService.deleteContext(contextId);
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // Configuration (Settings)
    // ==========================================

    @GetMapping("/config/ai")
    @Operation(summary = "AI設定取得")
    public ResponseEntity<Map<String, Object>> getAiConfig(@RequestParam(required = false) String tenantId) {
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("provider", settingService.getAiProvider(tenantId));
        config.put("model", settingService.getAiModel(tenantId));
        config.put("temperature", settingService.getAiTemperature(tenantId));
        config.put("maxTokens", settingService.getAiMaxTokens(tenantId));
        config.put("tavilyApiKey", settingService.getString(
                tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_TAVILY_API_KEY, null));
        return ResponseEntity.ok(config);
    }

    @PostMapping("/config/ai")
    @Operation(summary = "AI設定保存")
    public ResponseEntity<Void> saveAiConfig(
            @RequestParam(required = false) String tenantId,
            @RequestBody Map<String, Object> config) {

        // Helper to save
        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_AI_PROVIDER,
                config.get("provider"));
        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_AI_MODEL,
                config.get("model"));
        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_AI_TEMPERATURE,
                config.get("temperature"));
        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_AI_MAX_TOKENS,
                config.get("maxTokens"));
        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_TAVILY_API_KEY,
                config.get("tavilyApiKey"));

        return ResponseEntity.ok().build();
    }

    @GetMapping("/config/limits")
    @Operation(summary = "制限設定取得")
    public ResponseEntity<Map<String, Object>> getLimitsConfig(@RequestParam(required = false) String tenantId) {
        Map<String, Object> config = new java.util.HashMap<>();
        config.put("rpm", settingService.getRateLimitRpm(tenantId));
        config.put("rph", settingService.getRateLimitRph(tenantId));
        config.put("dailyQuota", settingService.getDailyTokenQuota(tenantId));
        return ResponseEntity.ok(config);
    }

    @PostMapping("/config/limits")
    @Operation(summary = "制限設定保存")
    public ResponseEntity<Void> saveLimitsConfig(
            @RequestParam(required = false) String tenantId,
            @RequestBody Map<String, Object> config) {

        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_LIMIT_RPM,
                config.get("rpm"));
        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_LIMIT_RPH,
                config.get("rph"));
        saveConfig(tenantId, jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_LIMIT_DAILY_QUOTA,
                config.get("dailyQuota"));

        return ResponseEntity.ok().build();
    }

    private void saveConfig(String tenantId, String key, Object value) {
        if (value == null)
            return;
        String valStr = String.valueOf(value);
        if (tenantId != null && !tenantId.isEmpty()) {
            settingService.saveTenantSetting(tenantId, key, valStr);
        } else {
            settingService.saveSystemSetting(key, valStr);
        }
    }

    // ==========================================
    // Limits & Quotas (Legacy / Read-only for Property based view if needed,
    // keeping for compat)
    // ==========================================

    @GetMapping("/limits/legacy")
    @Operation(summary = "現在の制限設定取得(Legacy)")
    public ResponseEntity<Map<String, Object>> getLimits() {
        Map<String, Object> limits = new java.util.HashMap<>();
        limits.put("rateLimit", properties.getRateLimit());
        limits.put("quota", properties.getQuota());
        return ResponseEntity.ok(limits);
    }

    @GetMapping("/token-usage/summary")
    @Operation(summary = "トークン使用量サマリ取得")
    public ResponseEntity<TokenUsageSummary> getTokenUsageSummary(
            @RequestParam String tenantId,
            @RequestParam(required = false) String date) {

        // JST explicitly
        LocalDate targetDate;
        if (date != null) {
            targetDate = LocalDate.parse(date);
        } else {
            targetDate = LocalDate.now(java.time.ZoneId.of("Asia/Tokyo"));
        }

        Long totalTokens = tokenUsageRepository.sumTotalTokensByTenantAndDate(tenantId, targetDate);

        return ResponseEntity.ok(TokenUsageSummary.builder()
                .tenantId(tenantId)
                .date(targetDate)
                .totalTokens(totalTokens != null ? totalTokens : 0L)
                .build());
    }

    @GetMapping("/token-usage/trend")
    @Operation(summary = "トークン使用量トレンド取得 (期間)")
    public ResponseEntity<List<MiraTokenUsage>> getTokenUsageTrend(
            @RequestParam String tenantId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<MiraTokenUsage> trend = tokenUsageRepository.findByTenantIdAndUsageDateBetween(tenantId, start, end);
        return ResponseEntity.ok(trend);
    }

    @Data
    @Builder
    public static class TokenUsageSummary {
        private String tenantId;
        private LocalDate date;
        private Long totalTokens;
    }

    // ==========================================
    // Knowledge Reindex Management
    // ==========================================

    @PostMapping("/knowledge/reindex")
    @Operation(summary = "ナレッジ一括再インデックス", description = "指定スコープのドキュメントを非同期で再インデックスします")
    public ResponseEntity<Map<String, Object>> reindexKnowledge(
            @RequestParam(defaultValue = "SYSTEM") String scope,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String userId) {

        jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore.Scope scopeEnum = jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore.Scope
                .valueOf(scope);

        String taskId = knowledgeBaseService.startBulkReindex(scopeEnum, tenantId, userId);

        return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "message", "Reindex job started",
                "scope", scope));
    }

    @GetMapping("/knowledge/reindex/progress/{fileId}")
    @Operation(summary = "インデックス進捗確認", description = "ファイルのインデックス処理進捗を取得します")
    public ResponseEntity<Map<String, Object>> getReindexProgress(@PathVariable String fileId) {
        var progress = knowledgeBaseService.getIndexingProgress(fileId);

        if (progress == null) {
            return ResponseEntity.ok(Map.of(
                    "fileId", fileId,
                    "status", "NOT_FOUND"));
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("fileId", progress.getFileId());
        result.put("status", progress.getStatus().name());
        result.put("errorMessage", progress.getErrorMessage());
        result.put("updatedAt", progress.getUpdatedAt());

        return ResponseEntity.ok(result);
    }

    // ==========================================
    // Data Management
    // ==========================================

    @GetMapping(value = "/conversations/export", produces = "text/csv")
    @Operation(summary = "会話履歴エクスポート (CSV)")
    public ResponseEntity<String> exportConversations(@RequestParam String tenantId) {
        String csvContent = dataExportService.exportConversationsToCsv(tenantId);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"mira_conversations_" + tenantId + ".csv\"")
                .body(csvContent);
    }

    // ==========================================
    // Model Management (Phase 3)
    // ==========================================

    /**
     * 利用可能なプロバイダ一覧取得.
     */
    @GetMapping("/providers")
    @Operation(summary = "利用可能なプロバイダ一覧取得", description = "設定されているAIプロバイダの一覧を取得します")
    public ResponseEntity<List<ProviderInfo>> getProviders() {
        List<String> availableProviders = aiProviderFactory.getAvailableProviders();

        List<ProviderInfo> providers = availableProviders.stream()
                .map(name -> {
                    String displayName = getProviderDisplayName(name);
                    boolean available = aiProviderFactory.getProvider(name).isPresent();

                    return ProviderInfo.builder()
                            .name(name)
                            .displayName(displayName)
                            .available(available)
                            .build();
                })
                .toList();

        return ResponseEntity.ok(providers);
    }

    /**
     * モデル一覧取得.
     */
    @GetMapping("/models")
    @Operation(summary = "モデル一覧取得", description = "プロバイダ別またはすべてのモデル一覧を取得します")
    public ResponseEntity<List<jp.vemi.mirel.apps.mira.domain.dao.entity.MiraModelRegistry>> getModels(
            @RequestParam(required = false) String provider) {

        if (provider != null && !provider.isEmpty()) {
            return ResponseEntity.ok(modelSelectionService.getAvailableModels(provider));
        } else {
            return ResponseEntity.ok(modelSelectionService.getAllAvailableModels());
        }
    }

    /**
     * プロバイダ表示名を取得.
     */
    private String getProviderDisplayName(String name) {
        return switch (name) {
            case "vertex-ai-gemini" -> "Vertex AI (Gemini)";
            case "github-models" -> "GitHub Models";
            case "azure-openai" -> "Azure OpenAI";
            case "mock" -> "Mock Provider";
            default -> name;
        };
    }

    /**
     * プロバイダ情報.
     */
    @Data
    @Builder
    public static class ProviderInfo {
        private String name;
        private String displayName;
        private boolean available;
    }
}
