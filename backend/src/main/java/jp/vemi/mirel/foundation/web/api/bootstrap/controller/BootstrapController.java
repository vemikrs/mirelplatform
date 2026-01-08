/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.bootstrap.controller;

import jp.vemi.mirel.foundation.web.api.admin.dto.AdminUserDto;
import jp.vemi.mirel.foundation.web.api.bootstrap.dto.BootstrapStatusResponse;
import jp.vemi.mirel.foundation.web.api.bootstrap.dto.CreateInitialAdminRequest;
import jp.vemi.mirel.foundation.web.api.bootstrap.service.BootstrapService;
import jp.vemi.mirel.foundation.web.api.bootstrap.service.BootstrapTokenService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Bootstrapコントローラー.
 * 
 * 初期セットアップ用のエンドポイントを提供します。
 * このエンドポイントは認証不要でアクセス可能です。
 */
@RestController
@RequestMapping("/api/bootstrap")
@RequiredArgsConstructor
public class BootstrapController {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapController.class);

    private final BootstrapService bootstrapService;
    private final BootstrapTokenService tokenService;

    /**
     * 起動時にトークンファイルを生成（必要な場合）
     */
    @PostConstruct
    public void init() {
        if (bootstrapService.isBootstrapAvailable()) {
            String token = tokenService.generateTokenIfNeeded();
            if (token != null) {
                logger.info("========================================");
                logger.info("BOOTSTRAP TOKEN GENERATED");
                logger.info("Check the token file at: {storage-dir}/bootstrap/setup-token.txt");
                logger.info("Or access /bootstrap in the browser");
                logger.info("========================================");
            }
        }
    }

    /**
     * Bootstrapステータスを取得
     * 
     * Bootstrap可能かどうかを返します。
     */
    @GetMapping("/status")
    public ResponseEntity<BootstrapStatusResponse> getStatus() {
        boolean available = bootstrapService.isBootstrapAvailable();

        BootstrapStatusResponse response = BootstrapStatusResponse.builder()
                .available(available)
                .message(available
                        ? "Bootstrap is available. Please create the initial admin user."
                        : "Bootstrap already completed. Initial admin already exists.")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 初期管理者を作成
     * 
     * ストレージに生成されたトークンを使用して認証します。
     */
    @PostMapping("/admin")
    public ResponseEntity<AdminUserDto> createInitialAdmin(
            @Valid @RequestBody CreateInitialAdminRequest request) {
        logger.info("Creating initial admin user");

        AdminUserDto adminUser = bootstrapService.createInitialAdmin(request);

        logger.info("Initial admin created successfully: userId={}", adminUser.getUserId());

        return ResponseEntity.ok(adminUser);
    }
}
