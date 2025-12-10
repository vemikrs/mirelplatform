/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OtpTokenリポジトリ.
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    /**
     * 有効なOTPトークンを検索
     * 
     * @param systemUserId
     *            SystemUser ID
     * @param purpose
     *            用途
     * @param isVerified
     *            検証済みフラグ
     * @param now
     *            現在日時
     * @return OTPトークン
     */
    Optional<OtpToken> findBySystemUserIdAndPurposeAndIsVerifiedAndExpiresAtAfter(
            UUID systemUserId,
            String purpose,
            Boolean isVerified,
            LocalDateTime now);

    /**
     * マジックリンクトークンから有効なOTPを検索
     * 
     * @param magicLinkToken
     *            マジックリンクトークン
     * @param isVerified
     *            検証済みフラグ
     * @param now
     *            現在日時
     * @return OTPトークン
     */
    Optional<OtpToken> findByMagicLinkTokenAndIsVerifiedAndExpiresAtAfter(
            String magicLinkToken,
            Boolean isVerified,
            LocalDateTime now);

    /**
     * ユーザー・用途別の全トークン取得
     * 
     * @param systemUserId
     *            SystemUser ID
     * @param purpose
     *            用途
     * @return OTPトークンリスト
     */
    List<OtpToken> findBySystemUserIdAndPurpose(UUID systemUserId, String purpose);

    /**
     * ユーザー・用途別の未検証トークン取得
     * 
     * @param systemUserId
     *            SystemUser ID
     * @param purpose
     *            用途
     * @param isVerified
     *            検証済みフラグ
     * @return OTPトークンリスト
     */
    List<OtpToken> findBySystemUserIdAndPurposeAndIsVerifiedFalse(UUID systemUserId, String purpose);

    /**
     * 古い未検証トークンを無効化
     * 
     * @param systemUserId
     *            SystemUser ID
     * @param purpose
     *            用途
     * @return 更新件数
     */
    @Modifying
    @Query("UPDATE OtpToken o SET o.isVerified = true WHERE o.systemUserId = :systemUserId AND o.purpose = :purpose AND o.isVerified = false")
    int invalidatePreviousTokens(
            @Param("systemUserId") UUID systemUserId,
            @Param("purpose") String purpose);

    /**
     * 期限切れトークンを削除
     * 
     * @param expirationTime
     *            期限日時
     * @return 削除件数
     */
    @Modifying
    int deleteByExpiresAtBefore(LocalDateTime expirationTime);
}
