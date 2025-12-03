/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FlowExecutionService {

    @Autowired
    private FlowManageService flowManageService;

    @Autowired
    private ObjectMapper objectMapper;

    public void execute(String flowId, Map<String, Object> context) {
        StuFlow flow = flowManageService.getFlow(flowId);
        String definition = flow.getDefinition();

        try {
            JsonNode root = objectMapper.readTree(definition);
            JsonNode nodes = root.get("nodes");
            JsonNode edges = root.get("edges");

            if (nodes == null || !nodes.isArray()) {
                System.out.println("No nodes found in flow definition.");
                return;
            }

            // Simple execution logic: Find start node and traverse
            // For now, just print the nodes as a proof of concept
            System.out.println("Executing Flow: " + flow.getFlowName());
            System.out.println("Context: " + context);

            for (JsonNode node : nodes) {
                String type = node.get("type").asText();
                String label = node.get("data").get("label").asText();
                System.out.println(" - Visiting Node: " + label + " (" + type + ")");

                if ("email".equals(type)) {
                    System.out.println("   -> Sending Email...");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute flow", e);
        }
    }
}
