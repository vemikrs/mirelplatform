/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.service;

import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.FeatureStatus;
import jp.vemi.mirel.foundation.abst.dao.repository.FeatureFlagRepository;
import jp.vemi.mirel.foundation.web.api.admin.dto.CreateFeatureFlagRequest;
import jp.vemi.mirel.foundation.web.api.admin.dto.FeatureFlagDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.FeatureFlagListResponse;
import jp.vemi.mirel.foundation.web.api.admin.dto.UpdateFeatureFlagRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * フィーチャーフラグ管理サービス.
 */
@Service
public class FeatureFlagService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    /**
     * フィーチャーフラグ一覧取得（ページング対応）
     */
    @Transactional(readOnly = true)
    public FeatureFlagListResponse listFeatureFlags(
            int page, 
            int size, 
            String applicationId,
            FeatureStatus status,
            Boolean inDevelopment,
            String searchTerm) {
        
        logger.info("List feature flags: page={}, size={}, applicationId={}, status={}, inDevelopment={}, searchTerm={}", 
            page, size, applicationId, status, inDevelopment, searchTerm);

        Pageable pageable = PageRequest.of(page, size, Sort.by("featureKey").ascending());
        
        Page<FeatureFlag> flagPage = featureFlagRepository.findWithFilters(
            applicationId,
            status,
            inDevelopment,
            searchTerm,
            pageable);

        List<FeatureFlagDto> featureDtos = flagPage.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

        return FeatureFlagListResponse.builder()
            .features(featureDtos)
            .page(page)
            .size(size)
            .totalElements(flagPage.getTotalElements())
            .totalPages(flagPage.getTotalPages())
            .build();
    }

    /**
     * フィーチャーフラグ詳細取得
     */
    @Transactional(readOnly = true)
    public FeatureFlagDto getFeatureFlagById(String id) {
        logger.info("Get feature flag by id: {}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
            .filter(f -> !Boolean.TRUE.equals(f.getDeleteFlag()))
            .orElseThrow(() -> new RuntimeException("Feature flag not found: " + id));

        return convertToDto(flag);
    }

    /**
     * featureKeyでフィーチャーフラグ取得
     */
    @Transactional(readOnly = true)
    public FeatureFlagDto getFeatureFlagByKey(String featureKey) {
        logger.info("Get feature flag by key: {}", featureKey);

        FeatureFlag flag = featureFlagRepository.findByFeatureKeyAndDeleteFlagFalse(featureKey)
            .orElseThrow(() -> new RuntimeException("Feature flag not found: " + featureKey));

        return convertToDto(flag);
    }

    /**
     * featureKeyの存在チェック
     */
    @Transactional(readOnly = true)
    public boolean existsByFeatureKey(String featureKey) {
        return featureFlagRepository.existsByFeatureKeyAndDeleteFlagFalse(featureKey);
    }

    /**
     * フィーチャーフラグ作成
     */
    @Transactional
    public FeatureFlagDto createFeatureFlag(CreateFeatureFlagRequest request, String userId) {
        logger.info("Create feature flag: featureKey={}", request.getFeatureKey());

        // 重複チェック
        if (existsByFeatureKey(request.getFeatureKey())) {
            throw new RuntimeException("Feature key already exists: " + request.getFeatureKey());
        }

        FeatureFlag flag = new FeatureFlag();
        flag.setId(UUID.randomUUID().toString());
        flag.setFeatureKey(request.getFeatureKey());
        flag.setFeatureName(request.getFeatureName());
        flag.setDescription(request.getDescription());
        flag.setApplicationId(request.getApplicationId());
        flag.setStatus(request.getStatus() != null ? request.getStatus() : FeatureStatus.STABLE);
        flag.setInDevelopment(request.getInDevelopment() != null ? request.getInDevelopment() : false);
        flag.setRequiredLicenseTier(request.getRequiredLicenseTier());
        flag.setEnabledByDefault(request.getEnabledByDefault() != null ? request.getEnabledByDefault() : true);
        flag.setEnabledForUserIds(request.getEnabledForUserIds());
        flag.setEnabledForTenantIds(request.getEnabledForTenantIds());
        flag.setRolloutPercentage(request.getRolloutPercentage() != null ? request.getRolloutPercentage() : 100);
        flag.setLicenseResolveStrategy(request.getLicenseResolveStrategy());
        flag.setMetadata(request.getMetadata());
        flag.setCreateUserId(userId);

        flag = featureFlagRepository.save(flag);

        logger.info("Feature flag created successfully: id={}, featureKey={}", flag.getId(), flag.getFeatureKey());

        return convertToDto(flag);
    }

    /**
     * フィーチャーフラグ更新
     */
    @Transactional
    public FeatureFlagDto updateFeatureFlag(String id, UpdateFeatureFlagRequest request, String userId) {
        logger.info("Update feature flag: id={}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
            .filter(f -> !Boolean.TRUE.equals(f.getDeleteFlag()))
            .orElseThrow(() -> new RuntimeException("Feature flag not found: " + id));

        if (request.getFeatureName() != null) {
            flag.setFeatureName(request.getFeatureName());
        }
        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }
        if (request.getApplicationId() != null) {
            flag.setApplicationId(request.getApplicationId());
        }
        if (request.getStatus() != null) {
            flag.setStatus(request.getStatus());
        }
        if (request.getInDevelopment() != null) {
            flag.setInDevelopment(request.getInDevelopment());
        }
        if (request.getRequiredLicenseTier() != null) {
            flag.setRequiredLicenseTier(request.getRequiredLicenseTier());
        }
        if (request.getEnabledByDefault() != null) {
            flag.setEnabledByDefault(request.getEnabledByDefault());
        }
        if (request.getEnabledForUserIds() != null) {
            flag.setEnabledForUserIds(request.getEnabledForUserIds());
        }
        if (request.getEnabledForTenantIds() != null) {
            flag.setEnabledForTenantIds(request.getEnabledForTenantIds());
        }
        if (request.getRolloutPercentage() != null) {
            flag.setRolloutPercentage(request.getRolloutPercentage());
        }
        if (request.getLicenseResolveStrategy() != null) {
            flag.setLicenseResolveStrategy(request.getLicenseResolveStrategy());
        }
        if (request.getMetadata() != null) {
            flag.setMetadata(request.getMetadata());
        }
        flag.setUpdateUserId(userId);

        flag = featureFlagRepository.save(flag);

        logger.info("Feature flag updated successfully: id={}", id);

        return convertToDto(flag);
    }

    /**
     * フィーチャーフラグ削除（論理削除）
     */
    @Transactional
    public void deleteFeatureFlag(String id, String userId) {
        logger.info("Delete feature flag: id={}", id);

        FeatureFlag flag = featureFlagRepository.findById(id)
            .filter(f -> !Boolean.TRUE.equals(f.getDeleteFlag()))
            .orElseThrow(() -> new RuntimeException("Feature flag not found: " + id));

        flag.setDeleteFlag(true);
        flag.setUpdateUserId(userId);
        featureFlagRepository.save(flag);

        logger.info("Feature flag deleted successfully: id={}", id);
    }

    /**
     * 有効なフィーチャーフラグ取得（ユーザー向けAPI用）
     */
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getAvailableFeatures(String userId, String tenantId) {
        logger.debug("Get available features for userId={}, tenantId={}", userId, tenantId);

        List<FeatureFlag> flags = featureFlagRepository.findEffectiveFeatures(userId, tenantId);

        return flags.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * アプリケーションごとの有効なフィーチャーフラグ取得
     */
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getAvailableFeaturesForApplication(
            String applicationId, String userId, String tenantId) {
        logger.debug("Get available features for app={}, userId={}, tenantId={}", 
            applicationId, userId, tenantId);

        List<FeatureFlag> flags = featureFlagRepository.findEffectiveFeaturesForApplication(
            applicationId, userId, tenantId);

        return flags.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * 開発中の機能を取得
     */
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getInDevelopmentFeatures() {
        logger.debug("Get in-development features");

        List<FeatureFlag> flags = featureFlagRepository.findByInDevelopmentTrueAndDeleteFlagFalse();

        return flags.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * FeatureFlagエンティティをDTOに変換
     */
    private FeatureFlagDto convertToDto(FeatureFlag flag) {
        return FeatureFlagDto.builder()
            .id(flag.getId())
            .featureKey(flag.getFeatureKey())
            .featureName(flag.getFeatureName())
            .description(flag.getDescription())
            .applicationId(flag.getApplicationId())
            .status(flag.getStatus())
            .inDevelopment(flag.getInDevelopment())
            .requiredLicenseTier(flag.getRequiredLicenseTier())
            .enabledByDefault(flag.getEnabledByDefault())
            .enabledForUserIds(flag.getEnabledForUserIds())
            .enabledForTenantIds(flag.getEnabledForTenantIds())
            .rolloutPercentage(flag.getRolloutPercentage())
            .licenseResolveStrategy(flag.getLicenseResolveStrategy())
            .metadata(flag.getMetadata())
            .createdAt(flag.getCreateDate() != null ? 
                Instant.ofEpochMilli(flag.getCreateDate().getTime()) : null)
            .updatedAt(flag.getUpdateDate() != null ? 
                Instant.ofEpochMilli(flag.getUpdateDate().getTime()) : null)
            .build();
    }
}
