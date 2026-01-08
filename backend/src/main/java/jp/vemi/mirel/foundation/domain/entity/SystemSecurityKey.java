/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * システムセキュリティ鍵エンティティ.
 * <p>
 * JWT署名用のRSA鍵ペアを管理する。
 * 秘密鍵はマスターキー(KEK)で暗号化されて保存される。
 * </p>
 */
@Entity
@Table(name = "mir_system_security_key")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSecurityKey {

    /**
     * 鍵ID (kid).
     * JWT署名時にヘッダに含まれる。
     */
    @Id
    @Column(name = "key_id", length = 64)
    private String keyId;

    /**
     * アルゴリズム (RS256等).
     */
    @Column(name = "algorithm", length = 16, nullable = false)
    private String algorithm;

    /**
     * 公開鍵 (PEM形式).
     * 平文で保存される。
     */
    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey;

    /**
     * 暗号化された秘密鍵.
     * マスターキー(KEK)でAES-256-GCM暗号化されている。
     */
    @Column(name = "private_key_enc", columnDefinition = "TEXT", nullable = false)
    private String privateKeyEncrypted;

    /**
     * 鍵のステータス.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private KeyStatus status;

    /**
     * 用途.
     */
    @Column(name = "use_purpose", length = 32, nullable = false)
    private String usePurpose;

    /**
     * 署名用として有効になった日時.
     */
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    /**
     * 署名用としての利用を終了した日時.
     */
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    /**
     * 作成日時.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 鍵ステータス.
     */
    public enum KeyStatus {
        /** 現在署名に使用される鍵 */
        CURRENT,
        /** 検証には使用できるが署名には使用しない鍵（猶予期間中） */
        VALID,
        /** 無効化された鍵 */
        EXPIRED
    }
}
