package jp.vemi.mirel.apps.studio.modeler.domain.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuCode;
import jp.vemi.mirel.apps.studio.modeler.domain.service.StuCodeService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CodeMaintenanceEngine {

    private final StuCodeService schemaCodeService;

    public List<StuCode> getCodeList(String groupId) {
        return schemaCodeService.findByGroupId(groupId);
    }

    public void saveCode(String groupId, List<StuCode> codes) {
        schemaCodeService.save(codes);
    }

    public void deleteCodeGroup(String groupId) {
        schemaCodeService.deleteByGroupId(groupId);
    }

    public List<String> getGroupList() {
        return schemaCodeService.getGroupList();
    }
}
