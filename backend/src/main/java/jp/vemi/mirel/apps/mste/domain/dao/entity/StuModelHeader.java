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
 * Studio Model Header Entity
 */
@Entity
@Table(name = "mste_model_header")
@Getter
@Setter
public class StuModelHeader {

    @Id
    @Column(name = "model_id", length = 64)
    private String modelId;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Status: DRAFT, PUBLISHED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
