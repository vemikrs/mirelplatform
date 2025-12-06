package jp.vemi.mirel.apps.studio.modeler.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;

@Repository
public interface StuModelRepository extends JpaRepository<StuModel, StuModel.PK> {
    List<StuModel> findByPk_ModelIdAndTenantId(String modelId, String tenantId);

    void deleteByPk_ModelIdAndTenantId(String modelId, String tenantId);

    boolean existsByPk_ModelIdAndTenantId(String modelId, String tenantId);
}
