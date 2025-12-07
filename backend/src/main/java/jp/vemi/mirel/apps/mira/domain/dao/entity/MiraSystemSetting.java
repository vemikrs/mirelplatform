/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira システム設定エンティティ.
 * <p>
 * システム全体のデフォルト設定を保持します。
 * </p>
 */
@Entity
@Table(name = "mir_mira_system_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraSystemSetting {

    /** 設定キー */
    @Id
    @Column(name = "setting_key", length = 100, nullable = false)
    private String key;

    /** 設定値（JSON または テキスト） */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String value;

    /** 更新日時 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
