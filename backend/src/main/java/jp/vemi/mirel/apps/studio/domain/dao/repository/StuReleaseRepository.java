/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.repository;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StuReleaseRepository extends JpaRepository<StuRelease, String> {
    List<StuRelease> findByModelIdOrderByVersionDesc(String modelId);

    Optional<StuRelease> findByModelIdAndVersion(String modelId, Integer version);

    Optional<StuRelease> findTopByModelIdOrderByVersionDesc(String modelId);
}
