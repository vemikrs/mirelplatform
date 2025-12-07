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
        return ResponseEntity.ok(Map.of(
                "provider", settingService.getAiProvider(tenantId),
                "model", settingService.getAiModel(tenantId),
                "temperature", settingService.getAiTemperature(tenantId),
                "maxTokens", settingService.getAiMaxTokens(tenantId)));
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

        return ResponseEntity.ok().build();
    }

    @GetMapping("/config/limits")
    @Operation(summary = "制限設定取得")
    public ResponseEntity<Map<String, Object>> getLimitsConfig(@RequestParam(required = false) String tenantId) {
        return ResponseEntity.ok(Map.of(
                "rpm", settingService.getRateLimitRpm(tenantId),
                "rph", settingService.getRateLimitRph(tenantId),
                "dailyQuota", settingService.getDailyTokenQuota(tenantId)));
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
        return ResponseEntity.ok(Map.of(
                "rateLimit", properties.getRateLimit(),
                "quota", properties.getQuota()));
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
}
