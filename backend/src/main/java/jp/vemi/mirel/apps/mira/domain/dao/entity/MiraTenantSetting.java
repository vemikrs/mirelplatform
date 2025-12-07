/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira テナント設定エンティティ.
 * <p>
 * テナントごとのオーバーライド設定を保持します。
 * </p>
 */
@Entity
@Table(name = "mir_mira_tenant_setting")
@IdClass(MiraTenantSetting.PK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraTenantSetting {

    /** テナントID */
    @Id
    @Column(length = 50, nullable = false)
    private String tenantId;

    /** 設定キー */
    @Id
    @Column(length = 100, nullable = false)
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements java.io.Serializable {
        private String tenantId;
        private String key;
    }
}
