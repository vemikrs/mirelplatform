/*
 * Copyright(c) 2021 mirelplatform All right reserved.
 */
package jp.vemi.mirel.apps.selenade.domain.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.selenade.domain.dao.entity.AruRuns;

/**
 * {@link AruRuns} のrepositoryです。
 */
@Repository
public interface AruRunsRepository extends JpaRepository<AruRuns, String> {


}
