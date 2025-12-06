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
 * Studio Flow Entity
 */
@Entity
@Table(name = "stu_flow")
@Getter
@Setter
public class StuFlow {

    @Id
    @Column(name = "flow_id", length = 64)
    private String flowId;

    @Column(name = "model_id", nullable = false, length = 64)
    private String modelId;

    @Column(name = "flow_name", nullable = false, length = 255)
    private String flowName;

    /**
     * Trigger Type: MANUAL, ON_CREATE, ON_UPDATE, ON_DELETE
     */
    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    /**
     * Flow Definition JSON
     */
    @Column(name = "definition", columnDefinition = "text")
    private String definition;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
