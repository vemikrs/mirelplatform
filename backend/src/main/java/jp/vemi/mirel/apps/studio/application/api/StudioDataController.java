/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.api;

import jp.vemi.mirel.apps.studio.domain.service.DynamicEntityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/studio/data")
public class StudioDataController {

    @org.springframework.beans.factory.annotation.Autowired
    private DynamicEntityService dynamicEntityService;

    @GetMapping("/{modelId}")
    public List<Map<String, Object>> list(@PathVariable String modelId) {
        return dynamicEntityService.findAll(modelId);
    }

    @GetMapping("/{modelId}/{recordId}")
    public Map<String, Object> get(@PathVariable String modelId, @PathVariable String recordId) {
        return dynamicEntityService.findById(modelId, recordId);
    }

    @PostMapping("/{modelId}")
    public void create(@PathVariable String modelId, @RequestBody Map<String, Object> data) {
        dynamicEntityService.insert(modelId, data);
    }

    @PutMapping("/{modelId}/{recordId}")
    public void update(@PathVariable String modelId, @PathVariable String recordId,
            @RequestBody Map<String, Object> data) {
        dynamicEntityService.update(modelId, recordId, data);
    }

    @DeleteMapping("/{modelId}/{recordId}")
    public void delete(@PathVariable String modelId, @PathVariable String recordId) {
        dynamicEntityService.delete(modelId, recordId);
    }
}
