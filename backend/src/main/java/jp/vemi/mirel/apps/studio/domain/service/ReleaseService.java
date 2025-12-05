/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuFlow;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuRelease;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuReleaseRepository;
import jp.vemi.mirel.apps.studio.modeler.domain.service.StuModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReleaseService {

    @Autowired
    private StuReleaseRepository releaseRepository;

    @Autowired
    private StuModelService studioModelService;

    @Autowired
    private FlowManageService flowManageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public StuRelease createRelease(String modelId) {
        // 1. Fetch current state
        StuModelHeader header = studioModelService.findHeader(modelId);
        List<StuModel> fields = studioModelService.findModels(modelId);
        List<StuFlow> flows = flowManageService.getFlowsByModelId(modelId);

        // 2. Create Snapshot
        Map<String, Object> snapshotMap = new HashMap<>();
        snapshotMap.put("header", header);
        snapshotMap.put("fields", fields);
        snapshotMap.put("flows", flows);

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize snapshot", e);
        }

        // 3. Determine Version
        Integer latestVersion = releaseRepository.findTopByModelIdOrderByVersionDesc(modelId)
                .map(StuRelease::getVersion)
                .orElse(0);
        Integer nextVersion = latestVersion + 1;

        // 4. Save Release
        StuRelease release = new StuRelease();
        release.setReleaseId(UUID.randomUUID().toString());
        release.setModelId(modelId);
        release.setVersion(nextVersion);
        release.setSnapshot(snapshotJson);

        return releaseRepository.save(release);
    }

    public List<StuRelease> getReleases(String modelId) {
        return releaseRepository.findByModelIdOrderByVersionDesc(modelId);
    }

    public StuRelease getRelease(String releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new NoSuchElementException("Release not found: " + releaseId));
    }

    // TODO: Implement getDiff
}
