/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.repository;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link StuField}
 */
@Repository
public interface StuFieldRepository extends JpaRepository<StuField, String> {

    List<StuField> findByModelIdOrderBySortOrder(String modelId);

    boolean existsByModelId(String modelId);
}
