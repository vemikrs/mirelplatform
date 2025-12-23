/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mira Search Log Entity.
 * Records search queries and feedback for debug and improvement.
 */
@Entity
@Table(name = "mir_mira_search_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 1000)
    private String query;

    @Column(length = 255)
    private String tenantId;

    @Column(length = 255)
    private String userId;

    private Double maxScore;

    @Column(length = 50)
    private String searchMethod; // VECTOR, HYBRID, KEYWORD

    @Column(length = 1000)
    private String feedback; // User feedback ("bad", "irrelevant", etc.)

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
