/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.application;

import jp.vemi.mirel.apps.mste.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.mste.domain.dao.entity.StuModelHeader;
import jp.vemi.mirel.apps.mste.domain.dao.repository.StuFieldRepository;
import jp.vemi.mirel.apps.mste.domain.dao.repository.StuModelHeaderRepository;
import jp.vemi.mirel.apps.mste.domain.service.SchemaEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaManageServiceTest {

    @Mock
    private StuModelHeaderRepository headerRepository;
    @Mock
    private StuFieldRepository fieldRepository;
    @Mock
    private SchemaEngineService schemaEngine;

    @InjectMocks
    private SchemaManageService service;

    @Test
    void publish_shouldPublishModel() {
        String modelId = "test_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("DRAFT");

        List<StuField> fields = Collections.emptyList();

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(fields);

        service.publish(modelId);

        verify(schemaEngine).createTable(model, fields);
        verify(headerRepository).save(model);
    }

    @Test
    void publish_shouldThrowExceptionIfModelNotFound() {
        String modelId = "unknown";
        when(headerRepository.findById(modelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(modelId))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void publish_shouldThrowExceptionIfAlreadyPublished() {
        String modelId = "published_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("PUBLISHED");

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> service.publish(modelId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already published");
    }
}
