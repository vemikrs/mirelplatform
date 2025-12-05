/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.abst.dao.entity.TenantSystemMaster;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantSystemMasterRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSystemSettingsService {

    private final TenantSystemMasterRepository repository;

    private static final String SYSTEM_TENANT_ID = "SYSTEM";

    /**
     * システム設定一覧を取得します
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSystemSettings() {
        List<TenantSystemMaster> settings = repository.findByTenantId(SYSTEM_TENANT_ID);
        Map<String, String> result = new HashMap<>();
        for (TenantSystemMaster setting : settings) {
            result.put(setting.getKey(), setting.getValue());
        }
        return result;
    }

    /**
     * システム設定を更新します
     */
    @Transactional
    public void updateSystemSettings(Map<String, String> newSettings) {
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            TenantSystemMaster setting = repository.findByTenantIdAndKey(SYSTEM_TENANT_ID, entry.getKey())
                    .orElse(new TenantSystemMaster());

            if (setting.getTenantId() == null) {
                setting.setTenantId(SYSTEM_TENANT_ID);
                setting.setKey(entry.getKey());
            }

            setting.setValue(entry.getValue());
            repository.save(setting);
        }
    }
}
