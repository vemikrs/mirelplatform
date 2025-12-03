/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.repository;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StuFlowRepository extends JpaRepository<StuFlow, String> {
    List<StuFlow> findByModelId(String modelId);
}
