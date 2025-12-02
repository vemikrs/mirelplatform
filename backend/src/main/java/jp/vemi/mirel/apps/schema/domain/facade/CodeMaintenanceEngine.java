package jp.vemi.mirel.apps.schema.domain.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicCode;
import jp.vemi.mirel.apps.schema.domain.service.SchemaCodeService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CodeMaintenanceEngine {

    private final SchemaCodeService schemaCodeService;

    public List<SchDicCode> getCodeList(String groupId) {
        return schemaCodeService.findByGroupId(groupId);
    }

    public void saveCode(String groupId, List<SchDicCode> codes) {
        schemaCodeService.save(codes);
    }

    public void deleteCodeGroup(String groupId) {
        schemaCodeService.deleteByGroupId(groupId);
    }

    public List<String> getGroupList() {
        return schemaCodeService.getGroupList();
    }
}
