/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.dto;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import lombok.Data;

import java.util.List;

@Data
public class UpdateDraftRequest {
    private String name;
    private String description;
    private List<StuField> fields;
}
