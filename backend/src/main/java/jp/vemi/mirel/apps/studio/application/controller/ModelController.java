package jp.vemi.mirel.apps.studio.application.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.apps.studio.domain.service.ModelService;
import jp.vemi.mirel.apps.studio.domain.service.ModelService.CreateModelRequest;
import jp.vemi.mirel.apps.studio.domain.service.ModelService.ModelDetailsDto;
import jp.vemi.mirel.apps.studio.domain.service.ModelService.UpdateModelHeaderRequest;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/studio/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    public ApiResponse<List<StuModelHeader>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(modelService.search(q, status));
    }

    @PostMapping
    public ApiResponse<StuModelHeader> create(@RequestBody CreateModelRequest request) {
        return ApiResponse.success(modelService.create(request));
    }

    @GetMapping("/{modelId}")
    public ApiResponse<ModelDetailsDto> get(@PathVariable String modelId) {
        return ApiResponse.success(modelService.getModel(modelId));
    }

    @PutMapping("/{modelId}")
    public ApiResponse<StuModelHeader> updateHeader(
            @PathVariable String modelId,
            @RequestBody UpdateModelHeaderRequest request) {
        return ApiResponse.success(modelService.updateHeader(modelId, request));
    }

    @DeleteMapping("/{modelId}")
    public ApiResponse<Void> delete(@PathVariable String modelId) {
        modelService.delete(modelId);
        return ApiResponse.success();
    }
}
