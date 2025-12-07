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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer;
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
    private final MiraDataExportService dataExportService; // Inject export service
    private final MiraAiProperties properties;

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

    // ==========================================
    // Limits & Quotas
    // ==========================================

    @GetMapping("/limits")
    @Operation(summary = "現在の制限設定取得")
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

        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        Long totalTokens = tokenUsageRepository.sumTotalTokensByTenantAndDate(tenantId, targetDate);

        return ResponseEntity.ok(TokenUsageSummary.builder()
                .tenantId(tenantId)
                .date(targetDate)
                .totalTokens(totalTokens != null ? totalTokens : 0L)
                .build());
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
