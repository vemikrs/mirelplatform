/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.dto;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import lombok.Data;

import java.util.List;

@Data
public class UpdateDraftRequest {
    private String name;
    private String description;
    private List<StuModel> fields;
}
