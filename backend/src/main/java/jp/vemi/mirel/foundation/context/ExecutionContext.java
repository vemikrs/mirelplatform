/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.context;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag;
import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * リクエストスコープ統一コンテキスト.
 * リクエストごとにユーザ、テナント、ライセンス、フィーチャーフラグ情報を管理
 */
@Setter
@Getter
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ExecutionContext {

    // ユーザ・テナント情報
    private User currentUser;
    private Tenant currentTenant;
    private List<ApplicationLicense> effectiveLicenses;
    private List<FeatureFlag> availableFeatures;
    private Map<String, Object> attributes = new HashMap<>();

    // リクエストメタデータ
    private String requestId;
    private String ipAddress;
    private String userAgent;
    private Instant requestTime;

    // ライセンスキャッシュ（リクエスト内）
    private final Map<String, Boolean> licenseCache = new ConcurrentHashMap<>();
    
    // フィーチャーフラグキャッシュ（リクエスト内）
    private final Map<String, Boolean> featureCache = new ConcurrentHashMap<>();

    /**
     * ライセンスを持っているかチェック
     * @param applicationId アプリケーションID
     * @param tier 必要なライセンスティア
     * @return ライセンスを持っている場合true
     */
    public boolean hasLicense(String applicationId, LicenseTier tier) {
        String cacheKey = applicationId + ":" + tier;
        return licenseCache.computeIfAbsent(cacheKey, k -> {
            if (effectiveLicenses == null || effectiveLicenses.isEmpty()) {
                return false;
            }
            return effectiveLicenses.stream()
                .anyMatch(l -> l.getApplicationId().equals(applicationId)
                    && l.getTier().ordinal() >= tier.ordinal()
                    && (l.getExpiresAt() == null || l.getExpiresAt().isAfter(Instant.now())));
        });
    }

    /**
     * 現在のユーザIDを取得
     */
    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }

    /**
     * 現在のテナントIDを取得
     */
    public String getCurrentTenantId() {
        return currentTenant != null ? currentTenant.getTenantId() : null;
    }

    /**
     * 認証済みかどうか
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * カスタム属性を設定
     */
    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * カスタム属性を取得
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) this.attributes.get(key);
    }

    /**
     * フィーチャーフラグを持っているかチェック
     * @param featureKey フィーチャーキー
     * @return フィーチャーを利用可能な場合true
     */
    public boolean hasFeature(String featureKey) {
        return featureCache.computeIfAbsent(featureKey, k -> {
            if (availableFeatures == null || availableFeatures.isEmpty()) {
                return false;
            }
            return availableFeatures.stream()
                .anyMatch(f -> f.getFeatureKey().equals(featureKey) 
                    && Boolean.TRUE.equals(f.getEnabledByDefault())
                    && !Boolean.TRUE.equals(f.getDeleteFlag()));
        });
    }

    /**
     * フィーチャーフラグを持っていて、かつ必要なライセンスも持っているかチェック
     * @param featureKey フィーチャーキー
     * @return フィーチャーを利用可能でライセンス条件も満たす場合true
     */
    public boolean hasFeatureWithLicense(String featureKey) {
        return featureCache.computeIfAbsent(featureKey + ":withLicense", k -> {
            if (availableFeatures == null || availableFeatures.isEmpty()) {
                return false;
            }
            return availableFeatures.stream()
                .filter(f -> f.getFeatureKey().equals(featureKey) 
                    && Boolean.TRUE.equals(f.getEnabledByDefault())
                    && !Boolean.TRUE.equals(f.getDeleteFlag()))
                .findFirst()
                .map(f -> {
                    // ライセンス不要の場合
                    if (f.getRequiredLicenseTier() == null) {
                        return true;
                    }
                    // ライセンスチェック
                    return hasLicense(f.getApplicationId(), f.getRequiredLicenseTier());
                })
                .orElse(false);
        });
    }

    /**
     * 利用可能なフィーチャーフラグ一覧を取得
     * @return 利用可能なフィーチャーフラグ一覧
     */
    public List<FeatureFlag> getAvailableFeatureFlags() {
        return availableFeatures != null ? availableFeatures : new ArrayList<>();
    }
}
