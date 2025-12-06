/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.repository;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link StuRelation}
 */
@Repository
public interface StuRelationRepository extends JpaRepository<StuRelation, String> {

    List<StuRelation> findBySourceModelId(String sourceModelId);

    List<StuRelation> findByTargetModelId(String targetModelId);
}
