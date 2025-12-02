/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Studio Field Entity
 */
@Entity
@Table(name = "stu_field")
@Getter
@Setter
public class StuField {

    @Id
    @Column(name = "field_id", length = 64)
    private String fieldId;

    @Column(name = "model_id", nullable = false, length = 64)
    private String modelId;

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    /**
     * Type: STRING, NUMBER, DATE, BOOLEAN, JSON, etc.
     */
    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
