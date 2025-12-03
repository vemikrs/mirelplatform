package jp.vemi.mirel.apps.studio.application.controller;

import jp.vemi.mirel.apps.studio.domain.service.DynamicEntityService;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/studio/data")
public class DataController {

    private final DynamicEntityService dynamicEntityService;

    public DataController(DynamicEntityService dynamicEntityService) {
        this.dynamicEntityService = dynamicEntityService;
    }

    @GetMapping("/{modelId}")
    public ApiResponse<List<Map<String, Object>>> getData(@PathVariable String modelId) {
        return ApiResponse.success(dynamicEntityService.findAll(modelId));
    }

    @GetMapping("/{modelId}/{id}")
    public ApiResponse<Map<String, Object>> getDataById(@PathVariable String modelId, @PathVariable String id) {
        return ApiResponse.success(dynamicEntityService.findById(modelId, id));
    }

    @PostMapping("/{modelId}")
    public ApiResponse<Void> createData(@PathVariable String modelId, @RequestBody Map<String, Object> content) {
        dynamicEntityService.insert(modelId, content);
        return ApiResponse.success();
    }

    @PutMapping("/{modelId}/{id}")
    public ApiResponse<Void> updateData(@PathVariable String modelId, @PathVariable String id,
            @RequestBody Map<String, Object> content) {
        dynamicEntityService.update(modelId, id, content);
        return ApiResponse.success();
    }

    @DeleteMapping("/{modelId}/{id}")
    public ApiResponse<Void> deleteData(@PathVariable String modelId, @PathVariable String id) {
        dynamicEntityService.delete(modelId, id);
        return ApiResponse.success();
    }
}
