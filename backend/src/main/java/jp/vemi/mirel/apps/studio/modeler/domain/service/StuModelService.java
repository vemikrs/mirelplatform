package jp.vemi.mirel.apps.studio.modeler.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuModelHeaderRepository;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuModelRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StuModelService {

    private final StuModelRepository schDicModelRepository;
    private final StuModelHeaderRepository schDicModelHeaderRepository;

    public List<StuModel> findModels(String modelId) {
        return schDicModelRepository.findByPk_ModelIdAndTenantId(modelId, TenantContext.getTenantId());
    }

    public StuModelHeader findHeader(String modelId) {
        return schDicModelHeaderRepository.findByModelIdAndTenantId(modelId, TenantContext.getTenantId()).orElse(null);
    }

    public void saveModel(List<StuModel> models) {
        String tenantId = TenantContext.getTenantId();
        models.forEach(model -> {
            if (model.getTenantId() == null) {
                model.setTenantId(tenantId);
            } else if (!model.getTenantId().equals(tenantId)) {
                throw new SecurityException("Cannot save model field for another tenant");
            }
        });
        schDicModelRepository.saveAll(models);
    }

    public void saveHeader(StuModelHeader header) {
        if (header.getTenantId() == null) {
            header.setTenantId(TenantContext.getTenantId());
        }
        // Ensure we are not overwriting another tenant's header
        if (schDicModelHeaderRepository.existsById(header.getModelId())) {
            schDicModelHeaderRepository.findByModelIdAndTenantId(header.getModelId(), TenantContext.getTenantId())
                    .orElseThrow(() -> new SecurityException("Access denied to model header: " + header.getModelId()));
        }
        schDicModelHeaderRepository.save(header);
    }

    public StuModelHeader createDraft(String name, String description) {
        StuModelHeader model = new StuModelHeader();
        model.setModelId(java.util.UUID.randomUUID().toString());
        model.setModelName(name);
        model.setDescription(description);
        model.setStatus("DRAFT");
        model.setModelType("ENTITY");
        model.setTenantId(TenantContext.getTenantId());
        model.setVersion(1);
        return schDicModelHeaderRepository.save(model);
    }

    public void updateDraft(String modelId, String name, String description, List<StuModel> fields) {
        String tenantId = TenantContext.getTenantId();
        StuModelHeader model = schDicModelHeaderRepository.findByModelIdAndTenantId(modelId, tenantId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Model not found: " + modelId));

        if ("PUBLISHED".equals(model.getStatus())) {
            throw new IllegalStateException("Cannot update published model. Create a new version.");
        }

        model.setModelName(name);
        model.setDescription(description);
        schDicModelHeaderRepository.save(model);

        // Replace fields
        List<StuModel> existingFields = schDicModelRepository.findByPk_ModelIdAndTenantId(modelId, tenantId);
        schDicModelRepository.deleteAll(existingFields);

        fields.forEach(field -> {
            if (field.getPk() == null) {
                field.setPk(new StuModel.PK());
            }
            field.getPk().setModelId(modelId);
            if (field.getPk().getFieldId() == null) {
                field.getPk().setFieldId(java.util.UUID.randomUUID().toString());
            }
            field.setTenantId(tenantId);
        });
        schDicModelRepository.saveAll(fields);
    }

    public List<StuModelHeader> findAll() {
        return schDicModelHeaderRepository.findByTenantId(TenantContext.getTenantId());
    }

    public void publish(String modelId) {
        String tenantId = TenantContext.getTenantId();
        StuModelHeader model = schDicModelHeaderRepository.findByModelIdAndTenantId(modelId, tenantId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Model not found: " + modelId));
        // TODO: Call SchemaEngine to create table
        model.setStatus("PUBLISHED");
        schDicModelHeaderRepository.save(model);
    }

    public void deleteModel(String modelId) {
        String tenantId = TenantContext.getTenantId();
        // Delete header
        schDicModelHeaderRepository.deleteByModelIdAndTenantId(modelId, tenantId);
        // Delete fields
        List<StuModel> fields = schDicModelRepository.findByPk_ModelIdAndTenantId(modelId, tenantId);
        schDicModelRepository.deleteAll(fields);
    }
}
