/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer.ContextScope;
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
     * 同一カテゴリの場合、より上位のスコープ（User &gt; Org &gt; Tenant &gt; System）で上書きされる。
     * </p>
     *
     * @param tenantId テナントID
     * @param organizationId 組織ID（nullable）
     * @param userId ユーザーID
     * @return マージされたコンテキスト（category → content）
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

        // 全階層のコンテキストを取得
        List<MiraContextLayer> layers = repository.findAllActiveContextsForUser(
                tenantId, organizationId, userId);

        if (log.isDebugEnabled()) {
            log.debug("[MiraContextLayerService] Found {} context layers", layers.size());
        }

        // スコープ・優先度でソートしてマージ
        // System(0) < Tenant(1) < Organization(2) < User(3)
        Map<String, String> merged = new HashMap<>();

        layers.stream()
                .sorted(Comparator
                        .comparingInt((MiraContextLayer l) -> l.getScope().ordinal())
                        .thenComparingInt(l -> Optional.ofNullable(l.getPriority()).orElse(0)))
                .forEach(layer -> {
                    merged.put(layer.getCategory(), layer.getContent());
                    if (log.isTraceEnabled()) {
                        log.trace("[MiraContextLayerService] Applied context: scope={}, category={}",
                                layer.getScope(), layer.getCategory());
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
    public String getMergedContextByCategory(
            String tenantId,
            String organizationId,
            String userId,
            String category) {
        
        Map<String, String> merged = buildMergedContext(tenantId, organizationId, userId);
        return merged.get(category);
    }

    /**
     * システムスコープのコンテキストを取得.
     *
     * @return システムコンテキスト一覧
     */
    @Transactional(readOnly = true)
    public List<MiraContextLayer> getSystemContexts() {
        return repository.findActiveSystemContexts();
    }

    /**
     * テナントスコープのコンテキストを取得.
     *
     * @param tenantId テナントID
     * @return テナントコンテキスト一覧
     */
    @Transactional(readOnly = true)
    public List<MiraContextLayer> getTenantContexts(String tenantId) {
        return repository.findActiveTenantContexts(tenantId);
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
            log.debug("[MiraContextLayerService] saveContext: scope={}, category={}",
                    layer.getScope(), layer.getCategory());
        }

        MiraContextLayer saved = repository.save(layer);
        log.info("[MiraContextLayerService] Saved context: id={}, scope={}, category={}",
                saved.getId(), saved.getScope(), saved.getCategory());
        return saved;
    }

    /**
     * ユーザーコンテキストを保存または更新（upsert）.
     *
     * @param userId ユーザーID
     * @param category カテゴリ
     * @param content コンテンツ
     * @return 保存されたエンティティ
     */
    @Transactional
    public MiraContextLayer saveOrUpdateUserContext(String userId, String category, String content) {
        if (log.isDebugEnabled()) {
            log.debug("[MiraContextLayerService] saveOrUpdateUserContext: userId={}, category={}",
                    userId, category);
        }

        // 既存のレコードを検索
        Optional<MiraContextLayer> existing = repository.findByScopeAndScopeIdAndCategory(
                ContextScope.USER, userId, category);
        
        MiraContextLayer layer;
        if (existing.isPresent()) {
            // 更新
            layer = existing.get();
            layer.setContent(content);
            log.debug("[MiraContextLayerService] Updating existing context: id={}", layer.getId());
        } else {
            // 新規作成
            layer = MiraContextLayer.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .scope(ContextScope.USER)
                    .scopeId(userId)
                    .category(category)
                    .content(content)
                    .priority(0)
                    .enabled(true)
                    .build();
            log.debug("[MiraContextLayerService] Creating new context");
        }

        MiraContextLayer saved = repository.save(layer);
        log.info("[MiraContextLayerService] Saved user context: id={}, category={}",
                saved.getId(), category);
        return saved;
    }

    /**
     * コンテキスト値をJSON Mapとしてパース.
     *
     * @param contextContent JSON文字列
     * @return パースされたMap
     */
    public Map<String, Object> parseContextContent(String contextContent) {
        if (contextContent == null || contextContent.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(contextContent, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("[MiraContextLayerService] Failed to parse context content as JSON", e);
            return Map.of("raw", contextContent);
        }
    }

    /**
     * MapをJSON文字列に変換.
     *
     * @param value 変換するMap
     * @return JSON文字列
     */
    public String toJsonContextContent(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[MiraContextLayerService] Failed to serialize context content to JSON", e);
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

        contexts.forEach((category, content) -> {
            sb.append("## ").append(category).append("\n");
            sb.append(content).append("\n\n");
        });

        if (log.isDebugEnabled()) {
            log.debug("[MiraContextLayerService] Built context prompt addition: {} chars",
                    sb.length());
        }

        return sb.toString();
    }
}
