package jp.vemi.mirel.apps.studio.domain.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuModelHeaderRepository;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuFlow;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.modeler.domain.service.StuModelService;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing Studio Models (REST).
 */
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuRelease;
import jp.vemi.mirel.apps.studio.domain.service.ReleaseService;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final StuModelService stuModelService;
    private final StuModelHeaderRepository modelHeaderRepository;
    private final FlowManageService flowManageService;
    private final ReleaseService releaseService;

    /**
     * Search models.
     * 
     * @param query
     *            Search query (optional)
     * @param status
     *            Status filter (optional)
     * @return List of model headers
     */
    public List<StuModelHeader> search(String query, String status) {
        // TODO: Implement proper search with query and status
        // For now, return all
        return stuModelService.findAll();
    }

    /**
     * Get model details (Draft).
     * 
     * @param modelId
     *            Model ID
     * @return Model details including fields and flows
     */
    public ModelDetailsDto getModel(String modelId) {
        StuModelHeader header = stuModelService.findHeader(modelId);
        List<StuModel> fields = stuModelService.findModels(modelId);
        List<StuFlow> flows = flowManageService.getFlowsByModelId(modelId);

        return new ModelDetailsDto(header, fields, flows);
    }

    /**
     * Create a new model.
     * 
     * @param request
     *            Creation request
     * @return Created model header
     */
    @Transactional
    public StuModelHeader create(CreateModelRequest request) {
        // Validation: Check if ID exists
        if (modelHeaderRepository.existsById(request.modelId())) {
            throw new IllegalArgumentException("Model ID already exists: " + request.modelId());
        }

        StuModelHeader header = StuModelHeader.builder()
                .modelId(request.modelId())
                .modelName(request.modelName())
                .modelType(request.modelType())
                .modelCategory(request.modelCategory())
                .description(request.description())
                .status("DRAFT") // Initial status
                .tenantId("default") // TODO: Multi-tenant support
                .version(1)
                .build();

        stuModelService.saveHeader(header);
        return header;
    }

    /**
     * Update model header.
     * 
     * @param modelId
     *            Model ID
     * @param request
     *            Update request
     * @return Updated model header
     */
    @Transactional
    public StuModelHeader updateHeader(String modelId, UpdateModelHeaderRequest request) {
        StuModelHeader header = stuModelService.findHeader(modelId);

        if (request.modelName() != null)
            header.setModelName(request.modelName());
        if (request.description() != null)
            header.setDescription(request.description());
        if (request.isHidden() != null)
            header.setIsHidden(request.isHidden());

        stuModelService.saveHeader(header);
        return header;
    }

    /**
     * Update draft (Fields/Flows).
     * 
     * @param modelId
     *            Model ID
     * @param request
     *            Update request
     */
    @Transactional
    public void updateDraft(String modelId, UpdateDraftRequest request) {
        if (request.fields() != null) {
            // Manual field replacement
            stuModelService.updateDraft(modelId, request.headerName(), request.headerDescription(), request.fields());
        }
    }

    /**
     * Publish model.
     * 
     * @param modelId
     *            Model ID
     */
    @Transactional
    public void publish(String modelId) {
        releaseService.createRelease(modelId);
        // Update header status
        StuModelHeader header = stuModelService.findHeader(modelId);
        header.setStatus("PUBLISHED");
        stuModelService.saveHeader(header);
    }

    /**
     * Discard draft (Restore from Release).
     * 
     * @param modelId
     *            Model ID
     */
    @Transactional
    public void discardDraft(String modelId) {
        // Find latest release
        List<StuRelease> releases = releaseService.getReleases(modelId);
        if (releases.isEmpty()) {
            throw new IllegalStateException("No release found to restore from.");
        }
        // StuRelease latest = releases.get(0);
        // Restore from snapshot (TODO: Implement logic)
    }

    public record UpdateDraftRequest(String headerName, String headerDescription, List<StuModel> fields) {
    }

    /**
     * Delete model.
     * 
     * @param modelId
     *            Model ID
     */
    @Transactional
    public void delete(String modelId) {
        // This should cascade delete fields and flows if configured,
        // otherwise we need manual cleanup.
        // Existing service deleteModel handles header + fields.
        stuModelService.deleteModel(modelId);
        // Clean up flows
        // flowManageService.deleteByModelId(modelId); // TODO: Add this method to
        // FlowManageService if not handling cascade
    }

    // DTOs
    public record ModelDetailsDto(StuModelHeader header, List<StuModel> fields, List<StuFlow> flows) {
    }

    public record CreateModelRequest(String modelId, String modelName, String modelType, String modelCategory,
            String description) {
    }

    public record UpdateModelHeaderRequest(String modelName, String description, Boolean isHidden) {
    }
}
