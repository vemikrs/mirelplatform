/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Studio Relation Entity
 */
@Entity
@Table(name = "mste_relation")
@Getter
@Setter
public class StuRelation {

    @Id
    @Column(name = "relation_id", length = 64)
    private String relationId;

    @Column(name = "source_model_id", nullable = false, length = 64)
    private String sourceModelId;

    @Column(name = "target_model_id", nullable = false, length = 64)
    private String targetModelId;

    /**
     * Type: ONE_TO_MANY, MANY_TO_ONE, ONE_TO_ONE, MANY_TO_MANY
     */
    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
