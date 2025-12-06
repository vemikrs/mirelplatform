/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer.LayerType;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraContextLayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira コンテキストレイヤーサービス.
 * <p>
 * 階層的なコンテキスト（System / Tenant / Organization / User）を管理し、
 * マージされたコンテキストを提供する。
 * </p>
 * 
 * <p><b>優先度:</b> System &lt; Tenant &lt; Organization &lt; User</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraContextLayerService {

    private final MiraContextLayerRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 階層コンテキストをマージして最終コンテキストを生成.
     * <p>
     * 同一キーの場合、より上位のレイヤー（User &gt; Org &gt; Tenant &gt; System）で上書きされる。
     * </p>
     *
     * @param tenantId テナントID
     * @param organizationId 組織ID（nullable）
     * @param userId ユーザーID
     * @return マージされたコンテキスト（key → value）
     */
    @Transactional(readOnly = true)
    public Map<String, String> buildMergedContext(
            String tenantId,
            String organizationId,
            String userId) {
        
        if (log.isDebugEnabled()) {
            log.debug("[MiraContextLayerService] buildMergedContext: tenant={}, org={}, user={}",
                    tenantId, organizationId, userId);
        }

        LocalDateTime now = LocalDateTime.now();

        // 全階層のコンテキストを取得
        List<MiraContextLayer> layers = repository.findAllActiveContextsForUser(
                tenantId, organizationId, userId, now);

        if (log.isDebugEnabled()) {
            log.debug("[MiraContextLayerService] Found {} context layers", layers.size());
        }

        // レイヤー種別・優先度でソートしてマージ
        // System(0) < Tenant(1) < Organization(2) < User(3)
        Map<String, String> merged = new HashMap<>();

        layers.stream()
                .sorted(Comparator
                        .comparingInt((MiraContextLayer l) -> l.getLayerType().ordinal())
                        .thenComparingInt(l -> Optional.ofNullable(l.getPriority()).orElse(0)))
                .forEach(layer -> {
                    merged.put(layer.getContextKey(), layer.getContextValue());
                    if (log.isTraceEnabled()) {
                        log.trace("[MiraContextLayerService] Applied context: layer={}, key={}",
                                layer.getLayerType(), layer.getContextKey());
                    }
                });

        return merged;
    }

    /**
     * 特定カテゴリのコンテキストを取得.
     *
     * @param tenantId テナントID
     * @param organizationId 組織ID
     * @param userId ユーザーID
     * @param category カテゴリ（例: "terminology", "style"）
     * @return マージされたコンテキスト内容（なければnull）
     */
    @Transactional(readOnly = true)
    public String getMergedContextByKey(
            String tenantId,
            String organizationId,
            String userId,
            String category) {
        
        Map<String, String> merged = buildMergedContext(tenantId, organizationId, userId);
        return merged.get(category);
    }

    /**
     * システムレイヤーのコンテキストを取得.
     *
     * @return システムコンテキスト一覧
     */
    @Transactional(readOnly = true)
    public List<MiraContextLayer> getSystemContexts() {
        return repository.findActiveSystemContexts(LocalDateTime.now());
    }

    /**
     * テナントレイヤーのコンテキストを取得.
     *
     * @param tenantId テナントID
     * @return テナントコンテキスト一覧
     */
    @Transactional(readOnly = true)
    public List<MiraContextLayer> getTenantContexts(String tenantId) {
        return repository.findActiveTenantContexts(tenantId, LocalDateTime.now());
    }

    /**
     * コンテキストを保存または更新.
     *
     * @param layer コンテキストレイヤー
     * @return 保存されたエンティティ
     */
    @Transactional
    public MiraContextLayer saveContext(MiraContextLayer layer) {
        if (log.isDebugEnabled()) {
            log.debug("[MiraContextLayerService] saveContext: layer={}, key={}",
                    layer.getLayerType(), layer.getContextKey());
        }

        MiraContextLayer saved = repository.save(layer);
        log.info("[MiraContextLayerService] Saved context: id={}, layer={}, key={}",
                saved.getId(), saved.getLayerType(), saved.getContextKey());
        return saved;
    }

    /**
     * 期限切れコンテキストをクリーンアップ.
     *
     * @return 削除件数
     */
    @Transactional
    public int cleanupExpiredContexts() {
        int deleted = repository.deleteExpiredContexts(LocalDateTime.now());
        if (deleted > 0) {
            log.info("[MiraContextLayerService] Cleaned up {} expired contexts", deleted);
        }
        return deleted;
    }

    /**
     * コンテキスト値をJSON Mapとしてパース.
     *
     * @param contextValue JSON文字列
     * @return パースされたMap
     */
    public Map<String, Object> parseContextValue(String contextValue) {
        if (contextValue == null || contextValue.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(contextValue, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("[MiraContextLayerService] Failed to parse context value as JSON", e);
            return Map.of("raw", contextValue);
        }
    }

    /**
     * MapをJSON文字列に変換.
     *
     * @param value 変換するMap
     * @return JSON文字列
     */
    public String toJsonContextValue(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[MiraContextLayerService] Failed to serialize context value to JSON", e);
            return "{}";
        }
    }

    /**
     * 階層コンテキストをプロンプト用のテキストに変換.
     *
     * @param tenantId テナントID
     * @param organizationId 組織ID
     * @param userId ユーザーID
     * @return プロンプトに追加するコンテキストテキスト
     */
    @Transactional(readOnly = true)
    public String buildContextPromptAddition(
            String tenantId,
            String organizationId,
            String userId) {
        
        Map<String, String> contexts = buildMergedContext(tenantId, organizationId, userId);
        
        if (contexts.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n# Additional Context\n\n");

        contexts.forEach((key, value) -> {
            sb.append("## ").append(key).append("\n");
            sb.append(value).append("\n\n");
        });

        if (log.isDebugEnabled()) {
            log.debug("[MiraContextLayerService] Built context prompt addition: {} chars",
                    sb.length());
        }

        return sb.toString();
    }
}
