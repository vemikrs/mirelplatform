/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.dto;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SchemaDetailResponse {
    private String modelId;
    private String modelName;
    private String description;
    private String status;
    private Integer version;
    private LocalDateTime updatedAt;
    private List<StuField> fields;
}
