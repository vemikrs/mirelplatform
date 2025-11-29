package jp.vemi.mirel.apps.schema.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicModel;

@Repository
public interface SchDicModelRepository extends JpaRepository<SchDicModel, SchDicModel.PK> {
    List<SchDicModel> findByPkModelId(String modelId);
}
