/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage;

/**
 * Mira メッセージリポジトリ.
 */
@Repository
public interface MiraMessageRepository extends JpaRepository<MiraMessage, String> {

    /**
     * 会話セッションのメッセージ一覧を取得（作成日時順）.
     */
    List<MiraMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * 会話セッションのメッセージ件数を取得.
     */
    long countByConversationId(String conversationId);

    /**
     * 会話セッションの直近N件のメッセージを取得.
     */
    @Query("SELECT m FROM MiraMessage m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC")
    List<MiraMessage> findRecentByConversationId(@Param("conversationId") String conversationId);

    /**
     * 会話セッションのメッセージを削除.
     */
    @Modifying
    @Query("DELETE FROM MiraMessage m WHERE m.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * 指定日時より古いメッセージを削除.
     */
    @Modifying
    @Query("DELETE FROM MiraMessage m WHERE m.createdAt < :before")
    void deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
