/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Studio Release Entity
 * Stores a snapshot of the model definition at a specific version.
 */
@Entity
@Table(name = "stu_release")
@Getter
@Setter
public class StuRelease {

    @Id
    @Column(name = "release_id", length = 64)
    private String releaseId;

    @Column(name = "model_id", nullable = false, length = 64)
    private String modelId;

    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Snapshot JSON
     * Contains the full definition of Model (Header, Fields) and Flows at this
     * version.
     */
    @Column(name = "snapshot", nullable = false, columnDefinition = "text")
    private String snapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
