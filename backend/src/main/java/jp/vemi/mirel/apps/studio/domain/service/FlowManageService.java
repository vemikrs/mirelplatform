/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuFlow;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFlowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class FlowManageService {

    @Autowired
    private StuFlowRepository flowRepository;

    @Transactional
    public StuFlow createFlow(String modelId, String name, String triggerType) {
        StuFlow flow = new StuFlow();
        flow.setFlowId(UUID.randomUUID().toString());
        flow.setModelId(modelId);
        flow.setFlowName(name);
        flow.setTriggerType(triggerType);
        flow.setDefinition("{}"); // Initial empty definition
        return flowRepository.save(flow);
    }

    @Transactional
    public StuFlow updateFlow(String flowId, String name, String triggerType, String definition) {
        StuFlow flow = flowRepository.findById(flowId)
                .orElseThrow(() -> new NoSuchElementException("Flow not found: " + flowId));

        if (name != null)
            flow.setFlowName(name);
        if (triggerType != null)
            flow.setTriggerType(triggerType);
        if (definition != null)
            flow.setDefinition(definition);

        return flowRepository.save(flow);
    }

    @Transactional
    public void deleteFlow(String flowId) {
        if (!flowRepository.existsById(flowId)) {
            throw new NoSuchElementException("Flow not found: " + flowId);
        }
        flowRepository.deleteById(flowId);
    }

    public StuFlow getFlow(String flowId) {
        return flowRepository.findById(flowId)
                .orElseThrow(() -> new NoSuchElementException("Flow not found: " + flowId));
    }

    public List<StuFlow> getFlowsByModelId(String modelId) {
        return flowRepository.findByModelId(modelId);
    }
}
