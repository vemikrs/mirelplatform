/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * InvitationTokenリポジトリ.
 */
@Repository
public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {
    
    /**
     * トークンで検索
     * 
     * @param token 招待トークン
     * @return 招待トークン
     */
    Optional<InvitationToken> findByToken(String token);
    
    /**
     * トークン・メールアドレスで検索
     * 
     * @param token 招待トークン
     * @param email メールアドレス
     * @return 招待トークン
     */
    Optional<InvitationToken> findByTokenAndEmail(String token, String email);
    
    /**
     * テナント別招待取得
     * 
     * @param tenantId Tenant ID
     * @return 招待トークンリスト
     */
    List<InvitationToken> findByTenantId(UUID tenantId);
    
    /**
     * テナント・メールアドレス別の有効な招待取得
     * 
     * @param tenantId Tenant ID
     * @param email メールアドレス
     * @param isUsed 使用済みフラグ
     * @param now 現在日時
     * @return 招待トークンリスト
     */
    List<InvitationToken> findByTenantIdAndEmailAndIsUsedAndExpiresAtAfter(
        UUID tenantId, 
        String email, 
        Boolean isUsed, 
        LocalDateTime now
    );
    
    /**
     * 期限切れトークンを削除
     * 
     * @param expirationTime 期限日時
     * @return 削除件数
     */
    @Modifying
    int deleteByExpiresAtBefore(LocalDateTime expirationTime);
}
