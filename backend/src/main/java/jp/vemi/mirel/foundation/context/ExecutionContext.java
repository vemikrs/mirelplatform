/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.context;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * リクエストスコープ統一コンテキスト.
 * リクエストごとにユーザ、テナント、ライセンス情報を管理
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
    private Map<String, Object> attributes = new HashMap<>();

    // リクエストメタデータ
    private String requestId;
    private String ipAddress;
    private String userAgent;
    private Instant requestTime;

    // ライセンスキャッシュ（リクエスト内）
    private final Map<String, Boolean> licenseCache = new ConcurrentHashMap<>();

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
}
