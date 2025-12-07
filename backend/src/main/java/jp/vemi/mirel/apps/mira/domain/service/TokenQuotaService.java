/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraTokenUsage;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraTokenUsageRepository;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * トークンクォータサービス.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenQuotaService {

    private final MiraTokenUsageRepository usageRepository;
    private final MiraAiProperties properties;

    /**
     * クォータ制限チェック.
     *
     * @param tenantId
     *            テナントID
     * @param estimatedTokens
     *            推定トークン数
     * @throws RuntimeException
     *             クォータ超過時
     */
    @Transactional(readOnly = true)
    public void checkQuota(String tenantId, int estimatedTokens) {
        if (!properties.getQuota().isEnabled()) {
            return;
        }

        LocalDate today = LocalDate.now();
        Long usedToday = usageRepository.sumTotalTokensByTenantAndDate(tenantId, today);

        long dailyLimit = properties.getQuota().getDailyTokenLimit();
        if (usedToday + estimatedTokens > dailyLimit) {
            log.warn("Token quota exceeded for tenant: {}. Used: {}, Limit: {}, Estimated: {}",
                    tenantId, usedToday, dailyLimit, estimatedTokens);
            throw new RuntimeException("Daily token quota exceeded");
        }
    }

    /**
     * トークン使用量を記録.
     *
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @param conversationId
     *            会話ID
     * @param model
     *            モデル名
     * @param inputTokens
     *            入力トークン数
     * @param outputTokens
     *            出力トークン数
     */
    @Transactional
    public void consume(String tenantId, String userId, String conversationId, String model,
            int inputTokens, int outputTokens) {
        if (!properties.getQuota().isEnabled()) {
            return;
        }

        MiraTokenUsage usage = MiraTokenUsage.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .userId(userId)
                .conversationId(conversationId)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .model(model)
                .usageDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();

        usageRepository.save(usage);
    }
}
