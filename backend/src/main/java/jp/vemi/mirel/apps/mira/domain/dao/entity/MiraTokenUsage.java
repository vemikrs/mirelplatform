/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * トークン使用量.
 */
@Entity
@Table(name = "mir_mira_token_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiraTokenUsage {

    /** ID. */
    @Id
    @Column(name = "id")
    private String id;

    /** テナントID. */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** ユーザーID. */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** 会話ID. */
    @Column(name = "conversation_id")
    private String conversationId;

    /** 入力トークン数. */
    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    /** 出力トークン数. */
    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    /** 使用モデル. */
    @Column(name = "model", nullable = false)
    private String model;

    /** 使用日. */
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    /** 作成日時. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
