/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.controller;

import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.FeatureStatus;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.web.api.admin.dto.CreateFeatureFlagRequest;
import jp.vemi.mirel.foundation.web.api.admin.dto.FeatureFlagDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.FeatureFlagListResponse;
import jp.vemi.mirel.foundation.web.api.admin.dto.UpdateFeatureFlagRequest;
import jp.vemi.mirel.foundation.web.api.admin.service.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理者用フィーチャーフラグ管理APIコントローラ.
 */
@RestController
@RequestMapping("/admin/features")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeatureFlagController {

    private static final Logger logger = LoggerFactory.getLogger(AdminFeatureFlagController.class);

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private ExecutionContext executionContext;

    /**
     * フィーチャーフラグ一覧取得
     */
    @GetMapping
    public ResponseEntity<FeatureFlagListResponse> listFeatureFlags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String applicationId,
            @RequestParam(required = false) FeatureStatus status,
            @RequestParam(required = false) Boolean inDevelopment,
            @RequestParam(required = false) String q) {
        
        try {
            FeatureFlagListResponse response = featureFlagService.listFeatureFlags(
                page, size, applicationId, status, inDevelopment, q);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to list feature flags", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * フィーチャーフラグ詳細取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<FeatureFlagDto> getFeatureFlag(@PathVariable String id) {
        try {
            FeatureFlagDto flag = featureFlagService.getFeatureFlagById(id);
            return ResponseEntity.ok(flag);
        } catch (RuntimeException e) {
            logger.error("Failed to get feature flag: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get feature flag: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * featureKeyの存在チェック
     */
    @GetMapping("/check-key")
    public ResponseEntity<Map<String, Boolean>> checkFeatureKey(@RequestParam String featureKey) {
        try {
            boolean exists = featureFlagService.existsByFeatureKey(featureKey);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            logger.error("Failed to check feature key: {}", featureKey, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * フィーチャーフラグ作成
     */
    @PostMapping
    public ResponseEntity<?> createFeatureFlag(@RequestBody CreateFeatureFlagRequest request) {
        try {
            String userId = executionContext.getCurrentUserId();
            FeatureFlagDto flag = featureFlagService.createFeatureFlag(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(flag);
        } catch (RuntimeException e) {
            logger.error("Failed to create feature flag: {}", request.getFeatureKey(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create feature flag: {}", request.getFeatureKey(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * フィーチャーフラグ更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFeatureFlag(
            @PathVariable String id,
            @RequestBody UpdateFeatureFlagRequest request) {
        try {
            String userId = executionContext.getCurrentUserId();
            FeatureFlagDto flag = featureFlagService.updateFeatureFlag(id, request, userId);
            return ResponseEntity.ok(flag);
        } catch (RuntimeException e) {
            logger.error("Failed to update feature flag: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update feature flag: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * フィーチャーフラグ削除（論理削除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFeatureFlag(@PathVariable String id) {
        try {
            String userId = executionContext.getCurrentUserId();
            featureFlagService.deleteFeatureFlag(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Failed to delete feature flag: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to delete feature flag: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
