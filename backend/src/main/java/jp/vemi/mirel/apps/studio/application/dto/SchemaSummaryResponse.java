/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SchemaSummaryResponse {
    private String modelId;
    private String modelName;
    private String description;
    private String status;
    private Integer version;
    private LocalDateTime updatedAt;
}
