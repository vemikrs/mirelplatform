/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation.ConversationMode;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation.ConversationStatus;

/**
 * Mira 会話セッションリポジトリ.
 */
@Repository
public interface MiraConversationRepository extends JpaRepository<MiraConversation, String> {

    /**
     * テナント・ユーザで会話一覧を取得.
     */
    List<MiraConversation> findByTenantIdAndUserIdAndStatusOrderByLastActivityAtDesc(
            String tenantId, String userId, ConversationStatus status);

    /**
     * テナント・ユーザ・ステータスでページング取得.
     */
    Page<MiraConversation> findByTenantIdAndUserIdAndStatusOrderByLastActivityAtDesc(
            String tenantId, String userId, ConversationStatus status, Pageable pageable);

    /**
     * テナント・ユーザでページング取得.
     */
    Page<MiraConversation> findByTenantIdAndUserIdOrderByLastActivityAtDesc(
            String tenantId, String userId, Pageable pageable);

    /**
     * テナント会話一覧 (Admin export用)
     */
    Page<MiraConversation> findByTenantIdOrderByLastActivityAtDesc(
            String tenantId, Pageable pageable);

    /**
     * テナント・ユーザ・モードで直近の会話を取得.
     */
    Optional<MiraConversation> findFirstByTenantIdAndUserIdAndModeAndStatusOrderByLastActivityAtDesc(
            String tenantId, String userId, ConversationMode mode, ConversationStatus status);

    /**
     * 指定日時より古いクローズ済み会話を削除.
     */
    @Query("DELETE FROM MiraConversation c WHERE c.status = :status AND c.lastActivityAt < :before")
    void deleteByStatusAndLastActivityAtBefore(
            @Param("status") ConversationStatus status,
            @Param("before") LocalDateTime before);
}
