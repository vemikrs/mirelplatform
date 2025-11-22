/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * テナントメールドメインルールエンティティ.
 * テナント単位で許可/ブロックするメールドメインを管理
 */
@Entity
@Table(name = "mir_tenant_email_domain_rule", indexes = {
    @Index(name = "idx_tenant_domain", columnList = "tenant_id, domain", unique = true)
})
@Getter
@Setter
public class TenantEmailDomainRule {
    
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * Tenant ID
     */
    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;
    
    /**
     * メールドメイン (例: vemi.jp, gmail.com)
     */
    @Column(name = "domain", nullable = false, length = 255)
    private String domain;
    
    /**
     * ルールタイプ (ALLOW, BLOCK)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 10)
    private RuleType ruleType;
    
    /**
     * 備考
     */
    @Column(name = "description", length = 500)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
    
    /**
     * ルールタイプ
     */
    public enum RuleType {
        /**
         * 許可リスト - このドメインのみ登録可能
         */
        ALLOW,
        
        /**
         * ブロックリスト - このドメインは登録不可
         */
        BLOCK
    }
}
