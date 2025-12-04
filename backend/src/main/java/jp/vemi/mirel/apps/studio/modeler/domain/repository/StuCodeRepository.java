package jp.vemi.mirel.apps.studio.modeler.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuCode;

@Repository
public interface StuCodeRepository extends JpaRepository<StuCode, StuCode.PK> {
    List<StuCode> findByPk_GroupIdAndTenantId(String groupId, String tenantId);

    void deleteByPk_GroupIdAndTenantId(String groupId, String tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c.pk.groupId FROM StuCode c WHERE c.tenantId = :tenantId ORDER BY c.pk.groupId")
    List<String> findDistinctGroupIds(@org.springframework.data.repository.query.Param("tenantId") String tenantId);
}
