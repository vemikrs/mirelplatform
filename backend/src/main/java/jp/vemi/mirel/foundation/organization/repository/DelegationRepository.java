/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jp.vemi.mirel.foundation.organization.model.Delegation;

public interface DelegationRepository extends JpaRepository<Delegation, String> {

    @Query("SELECT d FROM Delegation d WHERE d.primaryUserId = :userId AND :targetDate BETWEEN d.startDate AND d.endDate")
    List<Delegation> findActiveByPrimaryUserIdAndDate(@Param("userId") String userId,
            @Param("targetDate") LocalDate targetDate);
}
