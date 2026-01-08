/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.foundation.domain.entity.SystemSecurityKey;
import jp.vemi.mirel.foundation.domain.entity.SystemSecurityKey.KeyStatus;

/**
 * システムセキュリティ鍵リポジトリ.
 */
@Repository
public interface SystemSecurityKeyRepository extends JpaRepository<SystemSecurityKey, String> {

    /**
     * 現在署名に使用する鍵を取得.
     *
     * @param usePurpose
     *            用途
     * @return 現在の鍵（存在する場合）
     */
    Optional<SystemSecurityKey> findByUsePurposeAndStatus(String usePurpose, KeyStatus status);

    /**
     * 検証に使用できる鍵一覧を取得.
     * CURRENT または VALID ステータスの鍵を返す。
     *
     * @param usePurpose
     *            用途
     * @return 検証可能な鍵リスト
     */
    @Query("SELECT k FROM SystemSecurityKey k WHERE k.usePurpose = :usePurpose AND k.status IN ('CURRENT', 'VALID') ORDER BY k.activatedAt DESC")
    List<SystemSecurityKey> findValidKeysByPurpose(String usePurpose);

    /**
     * 現在署名に使用する鍵を取得（ACCESS_TOKEN_SIGN用ショートカット）.
     */
    default Optional<SystemSecurityKey> findCurrentSigningKey() {
        return findByUsePurposeAndStatus("ACCESS_TOKEN_SIGN", KeyStatus.CURRENT);
    }

    /**
     * 検証可能な鍵一覧を取得（ACCESS_TOKEN_SIGN用ショートカット）.
     */
    default List<SystemSecurityKey> findValidSigningKeys() {
        return findValidKeysByPurpose("ACCESS_TOKEN_SIGN");
    }

    /**
     * 期限切れとしてマークすべき鍵を取得.
     *
     * @param usePurpose
     *            用途
     * @param gracePeriodDays
     *            猶予期間（日数）
     * @return 期限切れ対象の鍵リスト
     */
    @Query("SELECT k FROM SystemSecurityKey k WHERE k.usePurpose = :usePurpose AND k.status = 'VALID' AND k.retiredAt < CURRENT_TIMESTAMP - :gracePeriodDays * INTERVAL '1 day'")
    List<SystemSecurityKey> findExpiredKeys(String usePurpose, int gracePeriodDays);
}
