/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira コンテキストスナップショットエンティティ.
 * 
 * <p>メッセージ送信時の画面コンテキスト情報を保存します。</p>
 */
@Entity
@Table(name = "mir_mira_context_snapshot", indexes = {
    @Index(name = "idx_mir_mira_ctx_tenant_user", columnList = "tenantId, userId"),
    @Index(name = "idx_mir_mira_ctx_app_screen", columnList = "appId, screenId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraContextSnapshot {

    /** スナップショットID（UUID） */
    @Id
    @Column(length = 36)
    private String id;

    /** テナントID */
    @Column(length = 36, nullable = false)
    private String tenantId;

    /** ユーザID */
    @Column(length = 36, nullable = false)
    private String userId;

    /** アプリケーションID（studio / workflow / admin 等） */
    @Column(length = 50, nullable = false)
    private String appId;

    /** 画面ID */
    @Column(length = 100, nullable = false)
    private String screenId;

    /** システムロール（ROLE_ADMIN / ROLE_USER） */
    @Column(length = 30)
    private String systemRole;

    /** アプリケーションロール（SystemAdmin / Builder / Operator / Viewer） */
    @Column(length = 30)
    private String appRole;

    /** 画面固有コンテキスト（JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    /** 取得日時 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
