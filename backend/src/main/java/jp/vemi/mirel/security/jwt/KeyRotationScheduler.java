/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.domain.entity.SystemSecurityKey;
import jp.vemi.mirel.foundation.domain.entity.SystemSecurityKey.KeyStatus;
import jp.vemi.mirel.foundation.domain.repository.SystemSecurityKeyRepository;

/**
 * JWT署名鍵の自動ローテーションジョブ.
 * <p>
 * 定期的に実行され、以下のローテーションを行う:
 * <ol>
 * <li>現在の鍵が一定期間経過していれば新しい鍵を生成</li>
 * <li>旧鍵をVALID（検証のみ有効）に移行</li>
 * <li>古いVALID鍵をEXPIRED（無効）に移行</li>
 * </ol>
 * </p>
 */
@Component
public class KeyRotationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeyRotationScheduler.class);
    private static final String USE_PURPOSE = "ACCESS_TOKEN_SIGN";

    @Autowired(required = false)
    private JwtKeyManagerService keyManagerService;

    @Autowired
    private SystemSecurityKeyRepository keyRepository;

    /** ローテーション間隔（日数） */
    @Value("${mirel.security.jwt.rotation-period-days:30}")
    private int rotationPeriodDays;

    /** 猶予期間（日数）: VALID鍵がEXPIREDになるまでの期間 */
    @Value("${mirel.security.jwt.grace-period-days:7}")
    private int gracePeriodDays;

    /**
     * 毎日午前3時に実行.
     */
    @Scheduled(cron = "${mirel.security.jwt.rotation-cron:0 0 3 * * *}")
    @Transactional
    public void rotateKeysIfNeeded() {
        if (keyManagerService == null || !keyManagerService.isRs256Available()) {
            logger.debug("[KeyRotation] RS256 not available, skipping rotation check");
            return;
        }

        logger.info("[KeyRotation] Starting key rotation check...");

        try {
            // 1. 現在の鍵のローテーションチェック
            checkAndRotateCurrentKey();

            // 2. 古いVALID鍵の期限切れ処理
            expireOldValidKeys();

            // 3. キャッシュ再ロード
            keyManagerService.reloadKeyCache();

            logger.info("[KeyRotation] Key rotation check completed");
        } catch (Exception e) {
            logger.error("[KeyRotation] Error during key rotation: {}", e.getMessage(), e);
        }
    }

    /**
     * 現在の鍵がローテーション対象か確認し、必要なら新鍵生成.
     */
    private void checkAndRotateCurrentKey() {
        var currentKeyOpt = keyRepository.findCurrentSigningKey();

        if (currentKeyOpt.isEmpty()) {
            logger.warn("[KeyRotation] No current key found, generating new key");
            keyManagerService.generateAndStoreNewKey();
            return;
        }

        SystemSecurityKey currentKey = currentKeyOpt.get();
        LocalDateTime activatedAt = currentKey.getActivatedAt();

        if (activatedAt == null) {
            activatedAt = currentKey.getCreatedAt();
        }

        LocalDateTime rotationThreshold = LocalDateTime.now().minusDays(rotationPeriodDays);

        if (activatedAt.isBefore(rotationThreshold)) {
            logger.info("[KeyRotation] Current key {} is older than {} days, rotating",
                    currentKey.getKeyId(), rotationPeriodDays);

            // 現在の鍵をVALIDに変更
            currentKey.setStatus(KeyStatus.VALID);
            currentKey.setRetiredAt(LocalDateTime.now());
            keyRepository.save(currentKey);

            // 新しい鍵を生成
            keyManagerService.generateAndStoreNewKey();

            logger.info("[KeyRotation] Key rotation completed. Old key {} moved to VALID",
                    currentKey.getKeyId());
        } else {
            logger.debug("[KeyRotation] Current key {} is still within rotation period",
                    currentKey.getKeyId());
        }
    }

    /**
     * 猶予期間を過ぎた古いVALID鍵をEXPIREDに変更.
     */
    private void expireOldValidKeys() {
        LocalDateTime graceThreshold = LocalDateTime.now().minusDays(gracePeriodDays);

        List<SystemSecurityKey> validKeys = keyRepository.findValidKeysByPurpose(USE_PURPOSE);

        for (SystemSecurityKey key : validKeys) {
            if (key.getStatus() == KeyStatus.VALID && key.getRetiredAt() != null) {
                if (key.getRetiredAt().isBefore(graceThreshold)) {
                    logger.info("[KeyRotation] Expiring key {} (retired at {})",
                            key.getKeyId(), key.getRetiredAt());
                    key.setStatus(KeyStatus.EXPIRED);
                    keyRepository.save(key);
                }
            }
        }
    }

    /**
     * 手動でローテーションを強制実行.
     * 管理者API等から呼び出される想定。
     */
    @Transactional
    public void forceRotation() {
        if (keyManagerService == null || !keyManagerService.isRs256Available()) {
            throw new IllegalStateException("RS256 key management is not available");
        }

        logger.info("[KeyRotation] Force rotation requested");

        var currentKeyOpt = keyRepository.findCurrentSigningKey();
        if (currentKeyOpt.isPresent()) {
            SystemSecurityKey currentKey = currentKeyOpt.get();
            currentKey.setStatus(KeyStatus.VALID);
            currentKey.setRetiredAt(LocalDateTime.now());
            keyRepository.save(currentKey);
        }

        keyManagerService.generateAndStoreNewKey();
        keyManagerService.reloadKeyCache();

        logger.info("[KeyRotation] Force rotation completed");
    }
}
