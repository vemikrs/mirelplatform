/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.dao.repository;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeaderLegacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link StuModelHeaderLegacy}
 */
@Repository
public interface StuModelHeaderLegacyRepository extends JpaRepository<StuModelHeaderLegacy, String> {

    List<StuModelHeaderLegacy> findByStatus(String status);
}
