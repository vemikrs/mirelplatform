/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dao.repository;

import jp.vemi.mirel.apps.mste.domain.dao.entity.StuModelHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link StuModelHeader}
 */
@Repository
public interface StuModelHeaderRepository extends JpaRepository<StuModelHeader, String> {

    List<StuModelHeader> findByStatus(String status);
}
