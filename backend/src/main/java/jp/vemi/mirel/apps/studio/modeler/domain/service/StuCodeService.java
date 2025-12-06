package jp.vemi.mirel.apps.studio.modeler.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuCode;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuCodeRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StuCodeService {

    private final StuCodeRepository schDicCodeRepository;

    public List<StuCode> findByGroupId(String groupId) {
        return schDicCodeRepository.findByPk_GroupIdAndTenantId(groupId, TenantContext.getTenantId());
    }

    public void save(List<StuCode> codes) {
        String tenantId = TenantContext.getTenantId();
        codes.forEach(code -> {
            if (code.getTenantId() == null) {
                code.setTenantId(tenantId);
            } else if (!code.getTenantId().equals(tenantId)) {
                throw new SecurityException("Cannot save code for another tenant");
            }
        });
        schDicCodeRepository.saveAll(codes);
    }

    public void deleteByGroupId(String groupId) {
        schDicCodeRepository.deleteByPk_GroupIdAndTenantId(groupId, TenantContext.getTenantId());
    }

    public List<String> getGroupList() {
        return schDicCodeRepository.findDistinctGroupIds(TenantContext.getTenantId());
    }
}
