package jp.vemi.mirel.apps.schema.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicCode;
import jp.vemi.mirel.apps.schema.domain.repository.SchDicCodeRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SchemaCodeService {

    private final SchDicCodeRepository schDicCodeRepository;

    public List<SchDicCode> findByGroupId(String groupId) {
        return schDicCodeRepository.findByPkGroupId(groupId);
    }

    public void save(List<SchDicCode> codes) {
        codes.forEach(code -> {
            if (code.getTenantId() == null) {
                code.setTenantId(TenantContext.getTenantId());
            }
        });
        schDicCodeRepository.saveAll(codes);
    }

    public void deleteByGroupId(String groupId) {
        List<SchDicCode> codes = schDicCodeRepository.findByPkGroupId(groupId);
        schDicCodeRepository.deleteAll(codes);
    }
}
