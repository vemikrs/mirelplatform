/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraConversationRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraMessageRepository;
import jp.vemi.mirel.apps.mira.domain.dto.response.ExportDataResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ExportDataResponse.ConversationExport;
import jp.vemi.mirel.apps.mira.domain.dto.response.ExportDataResponse.ExportMetadata;
import jp.vemi.mirel.apps.mira.domain.dto.response.ExportDataResponse.MessageExport;
import jp.vemi.mirel.apps.mira.domain.dto.response.ExportDataResponse.UserContextExport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira エクスポートサービス.
 * <p>
 * ユーザー単位の会話履歴とユーザーコンテキストをエクスポートする機能を提供します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraExportService {

    private static final String EXPORT_FORMAT_VERSION = "1.0";

    private final MiraConversationRepository conversationRepository;
    private final MiraMessageRepository messageRepository;
    private final MiraContextLayerService contextLayerService;

    /**
     * ユーザー単位のデータをエクスポート.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @return エクスポートデータ
     */
    @Transactional(readOnly = true)
    public ExportDataResponse exportUserData(String tenantId, String userId) {
        log.info("[MiraExportService] Starting export for user: tenantId={}, userId={}",
                tenantId, userId);

        // 全会話を取得
        List<MiraConversation> conversations = conversationRepository
                .findByTenantIdAndUserIdOrderByLastActivityAtDesc(tenantId, userId, null)
                .getContent();

        log.debug("[MiraExportService] Found {} conversations", conversations.size());

        // 会話とメッセージをエクスポート形式に変換
        List<ConversationExport> conversationExports = new ArrayList<>();
        int totalMessages = 0;

        for (MiraConversation conv : conversations) {
            // 会話のメッセージを取得
            List<MiraMessage> messages = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conv.getId());

            totalMessages += messages.size();

            // メッセージをエクスポート形式に変換
            List<MessageExport> messageExports = messages.stream()
                    .map(this::toMessageExport)
                    .collect(Collectors.toList());

            // 会話をエクスポート形式に変換
            conversationExports.add(new ConversationExport(
                    conv.getId(),
                    conv.getTitle(),
                    conv.getMode() != null ? conv.getMode().name() : null,
                    toInstant(conv.getCreatedAt()),
                    toInstant(conv.getLastActivityAt()),
                    messageExports
            ));
        }

        // ユーザーコンテキストを取得
        UserContextExport userContextExport = exportUserContext(tenantId, userId);

        // メタデータを構築
        ExportMetadata metadata = new ExportMetadata(
                Instant.now(),
                userId,
                tenantId,
                conversations.size(),
                totalMessages,
                EXPORT_FORMAT_VERSION
        );

        log.info("[MiraExportService] Export completed: {} conversations, {} messages",
                conversations.size(), totalMessages);

        return new ExportDataResponse(metadata, conversationExports, userContextExport);
    }

    /**
     * ユーザーコンテキストをエクスポート.
     */
    private UserContextExport exportUserContext(String tenantId, String userId) {
        // マージされたコンテキストを取得
        Map<String, String> contexts = contextLayerService.buildMergedContext(
                tenantId, null, userId);

        // 主要カテゴリを抽出
        String terminology = contexts.getOrDefault("terminology", "");
        String style = contexts.getOrDefault("style", "");
        String workflow = contexts.getOrDefault("workflow", "");

        // その他のコンテキストを別途保存
        Map<String, String> additional = new HashMap<>(contexts);
        additional.remove("terminology");
        additional.remove("style");
        additional.remove("workflow");

        return new UserContextExport(terminology, style, workflow, additional);
    }

    /**
     * MiraMessage → MessageExport 変換.
     */
    private MessageExport toMessageExport(MiraMessage message) {
        Map<String, Object> metadata = new HashMap<>();

        if (message.getTokenCount() != null) {
            metadata.put("tokenCount", message.getTokenCount());
        }
        if (message.getUsedModel() != null) {
            metadata.put("usedModel", message.getUsedModel());
        }

        return new MessageExport(
                message.getId(),
                message.getSenderType() != null ? message.getSenderType().name() : "UNKNOWN",
                message.getContent(),
                message.getContentType() != null ? message.getContentType().name() : "PLAIN",
                toInstant(message.getCreatedAt()),
                metadata
        );
    }

    /**
     * LocalDateTime → Instant 変換.
     */
    private Instant toInstant(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
