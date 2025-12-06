/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.controller;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuFlow;
import jp.vemi.mirel.apps.studio.domain.service.FlowExecutionService;
import jp.vemi.mirel.apps.studio.domain.service.FlowManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/studio/flows")
public class FlowController {

    @Autowired
    private FlowManageService flowManageService;

    @Autowired
    private FlowExecutionService flowExecutionService;

    @GetMapping("/{modelId}")
    public List<StuFlow> getFlows(@PathVariable String modelId) {
        return flowManageService.getFlowsByModelId(modelId);
    }

    @PostMapping
    public StuFlow createFlow(@RequestBody Map<String, String> body) {
        String modelId = body.get("modelId");
        String name = body.get("name");
        String triggerType = body.get("triggerType");
        return flowManageService.createFlow(modelId, name, triggerType);
    }

    @PutMapping("/{flowId}")
    public StuFlow updateFlow(@PathVariable String flowId, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String triggerType = body.get("triggerType");
        String definition = body.get("definition");
        return flowManageService.updateFlow(flowId, name, triggerType, definition);
    }

    @DeleteMapping("/{flowId}")
    public void deleteFlow(@PathVariable String flowId) {
        flowManageService.deleteFlow(flowId);
    }

    @PostMapping("/{flowId}/execute")
    public void executeFlow(@PathVariable String flowId, @RequestBody Map<String, Object> context) {
        flowExecutionService.execute(flowId, context);
    }
}
