/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraTokenUsage;

/**
 * トークン使用量リポジトリ.
 */
@Repository
public interface MiraTokenUsageRepository extends JpaRepository<MiraTokenUsage, String> {

    /**
     * テナントと日付ごとの合計トークン数を取得.
     *
     * @param tenantId
     *            テナントID
     * @param usageDate
     *            使用日
     * @return 合計入力トークン数 + 合計出力トークン数
     */
    @Query("SELECT COALESCE(SUM(t.inputTokens + t.outputTokens), 0) FROM MiraTokenUsage t WHERE t.tenantId = :tenantId AND t.usageDate = :usageDate")
    Long sumTotalTokensByTenantAndDate(String tenantId, LocalDate usageDate);

    /**
     * テナントと日付で使用量を検索.
     */
    List<MiraTokenUsage> findByTenantIdAndUsageDate(String tenantId, LocalDate usageDate);
}
