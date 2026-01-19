/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.feature.controller;

import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.web.api.admin.dto.FeatureFlagDto;
import jp.vemi.mirel.foundation.web.api.admin.service.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.vemi.framework.util.SanitizeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ユーザー向けフィーチャーフラグAPIコントローラ.
 * 認証済みユーザーが利用可能な機能を取得するためのエンドポイント
 */
@RestController
@RequestMapping("/features")
public class FeatureController {

    private static final Logger logger = LoggerFactory.getLogger(FeatureController.class);

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private ExecutionContext executionContext;

    /**
     * 利用可能なフィーチャーフラグ一覧取得
     */
    @GetMapping("/available")
    public ResponseEntity<List<FeatureFlagDto>> getAvailableFeatures() {
        try {
            String userId = executionContext.getCurrentUserId();
            String tenantId = executionContext.getCurrentTenantId();

            List<FeatureFlagDto> features = featureFlagService.getAvailableFeatures(userId, tenantId);
            return ResponseEntity.ok(features);
        } catch (Exception e) {
            logger.error("Failed to get available features", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * アプリケーションごとの利用可能なフィーチャーフラグ取得
     */
    @GetMapping("/available/{applicationId}")
    public ResponseEntity<List<FeatureFlagDto>> getAvailableFeaturesForApplication(
            @PathVariable String applicationId) {
        try {
            String userId = executionContext.getCurrentUserId();
            String tenantId = executionContext.getCurrentTenantId();

            List<FeatureFlagDto> features = featureFlagService.getAvailableFeaturesForApplication(
                applicationId, userId, tenantId);
            return ResponseEntity.ok(features);
        } catch (Exception e) {
            logger.error("Failed to get available features for application: {}", SanitizeUtil.forLog(applicationId), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 開発中の機能一覧取得
     */
    @GetMapping("/in-development")
    public ResponseEntity<List<FeatureFlagDto>> getInDevelopmentFeatures() {
        try {
            List<FeatureFlagDto> features = featureFlagService.getInDevelopmentFeatures();
            return ResponseEntity.ok(features);
        } catch (Exception e) {
            logger.error("Failed to get in-development features", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
