/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira モデルレジストリエンティティ.
 * <p>
 * プロバイダごとの利用可能なAIモデル情報を管理します。
 * </p>
 */
@Entity
@Table(name = "mir_mira_model_registry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraModelRegistry {

    /** モデルID (例: vertex-ai-gemini:gemini-2.5-flash) */
    @Id
    @Column(length = 255, nullable = false)
    private String id;

    /** プロバイダ名 (vertex-ai-gemini, github-models等) */
    @Column(length = 100, nullable = false)
    private String provider;

    /** モデル名 (gemini-2.5-flash, gpt-4o等) */
    @Column(name = "model_name", length = 255, nullable = false)
    private String modelName;

    /** 表示名 (Gemini 2.5 Flash) */
    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    /** モデル説明 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 機能配列 (JSON文字列: ["TOOL_CALLING", "STREAMING", ...]) */
    @Column(columnDefinition = "TEXT")
    private String capabilities;

    /** 最大トークン数 */
    @Column(name = "max_tokens")
    private Integer maxTokens;

    /** コンテキストウィンドウサイズ */
    @Column(name = "context_window")
    private Integer contextWindow;

    /** 有効/無効 */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** 推奨モデルフラグ */
    @Column(name = "is_recommended")
    @Builder.Default
    private Boolean isRecommended = false;

    /** 実験的モデルフラグ */
    @Column(name = "is_experimental")
    @Builder.Default
    private Boolean isExperimental = false;

    /** その他メタデータ (JSON文字列) */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /** 作成日時 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新日時 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ヘルパーメソッド: JSON文字列をリストに変換

    /**
     * capabilities フィールド（JSON文字列）をリストに変換.
     * 
     * @return 機能のリスト
     */
    @Transient
    public List<String> getCapabilitiesList() {
        if (capabilities == null || capabilities.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(capabilities, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * リストを capabilities フィールド（JSON文字列）に変換.
     * 
     * @param list
     *            機能のリスト
     */
    @Transient
    public void setCapabilitiesList(List<String> list) {
        if (list == null) {
            this.capabilities = "[]";
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.capabilities = mapper.writeValueAsString(list);
        } catch (Exception e) {
            this.capabilities = "[]";
        }
    }
}
