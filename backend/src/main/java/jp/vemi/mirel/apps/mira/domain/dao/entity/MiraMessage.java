/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira メッセージエンティティ.
 * 
 * <p>
 * 会話内の個々のメッセージを管理します。
 * </p>
 */
@Entity
@Table(name = "mir_mira_message", indexes = {
        @Index(name = "idx_mir_mira_msg_conversation", columnList = "conversationId"),
        @Index(name = "idx_mir_mira_msg_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraMessage {

    /** メッセージID（UUID） */
    @Id
    @Column(length = 36)
    private String id;

    /** 会話セッションID */
    @Column(length = 36, nullable = false)
    private String conversationId;

    /** 送信者種別 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SenderType senderType;

    /** メッセージ本文 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 添付ファイル情報 (JSON) */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String attachedFiles;

    /** コンテンツタイプ */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ContentType contentType = ContentType.PLAIN;

    /** コンテキストスナップショットID（nullable） */
    @Column(length = 36)
    private String contextSnapshotId;

    /** ツール呼び出しID（SenderType=TOOLの場合の紐付け元） */
    @Column(length = 100)
    private String toolCallId;

    /** 使用モデル名（アシスタント応答時のみ） */
    @Column(length = 100)
    private String usedModel;

    /** トークン使用量（概算） */
    @Column
    private Integer tokenCount;

    /** 作成日時 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 送信者種別 */
    public enum SenderType {
        /** ユーザ */
        USER,
        /** AI アシスタント */
        ASSISTANT,
        /** システム */
        SYSTEM,
        /** ツール（Function Calling 結果） */
        TOOL
    }

    /** コンテンツタイプ */
    public enum ContentType {
        /** プレーンテキスト */
        PLAIN,
        /** Markdown */
        MARKDOWN,
        /** 構造化 JSON */
        STRUCTURED_JSON
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
