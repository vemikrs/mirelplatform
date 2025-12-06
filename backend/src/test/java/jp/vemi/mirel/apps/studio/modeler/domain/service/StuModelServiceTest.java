package jp.vemi.mirel.apps.studio.modeler.domain.service;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuModelHeaderRepository;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuModelRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StuModelServiceTest {

    @Mock
    private StuModelRepository schDicModelRepository;
    @Mock
    private StuModelHeaderRepository schDicModelHeaderRepository;

    @InjectMocks
    private StuModelService stuModelService;

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("tenant-1");
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    void createDraft() {
        StuModelHeader savedHeader = new StuModelHeader();
        savedHeader.setModelId("id-1");
        savedHeader.setModelName("Test Model");
        when(schDicModelHeaderRepository.save(any(StuModelHeader.class))).thenReturn(savedHeader);

        StuModelHeader result = stuModelService.createDraft("Test Model", "Description");

        assertNotNull(result);
        assertEquals("id-1", result.getModelId());
        assertEquals("Test Model", result.getModelName());
    }

    @Test
    void updateDraft() {
        String modelId = "id-1";
        StuModelHeader existingHeader = new StuModelHeader();
        existingHeader.setModelId(modelId);
        existingHeader.setStatus("DRAFT");
        existingHeader.setTenantId("tenant-1");

        when(schDicModelHeaderRepository.findByModelIdAndTenantId(modelId, "tenant-1"))
                .thenReturn(Optional.of(existingHeader));
        when(schDicModelRepository.findByPk_ModelIdAndTenantId(modelId, "tenant-1"))
                .thenReturn(Collections.emptyList());

        List<StuModel> fields = Collections.singletonList(StuModel.builder().fieldName("field1").build());

        stuModelService.updateDraft(modelId, "New Name", "New Desc", fields);

        verify(schDicModelHeaderRepository, times(1)).save(any(StuModelHeader.class));
        verify(schDicModelRepository, times(1)).deleteAll(anyList());
        verify(schDicModelRepository, times(1)).saveAll(anyList());
    }
}
