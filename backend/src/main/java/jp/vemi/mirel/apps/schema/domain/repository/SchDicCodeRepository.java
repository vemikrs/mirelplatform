package jp.vemi.mirel.apps.schema.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicCode;

@Repository
public interface SchDicCodeRepository extends JpaRepository<SchDicCode, SchDicCode.PK> {
    List<SchDicCode> findByPk_GroupIdAndTenantId(String groupId, String tenantId);

    void deleteByPk_GroupIdAndTenantId(String groupId, String tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c.pk.groupId FROM SchDicCode c WHERE c.tenantId = :tenantId ORDER BY c.pk.groupId")
    List<String> findDistinctGroupIds(@org.springframework.data.repository.query.Param("tenantId") String tenantId);
}
