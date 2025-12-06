/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.web.api.admin.dto.ApplicationModuleDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.TenantStatsDto;
import jp.vemi.mirel.foundation.web.api.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;

/**
 * システム統計情報API コントローラ.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService statsService;

    /**
     * テナント統計情報を取得.
     */
    @GetMapping("/stats/tenants")
    public ResponseEntity<TenantStatsDto> getTenantStats() {
        return ResponseEntity.ok(statsService.getTenantStats());
    }

    /**
     * アプリケーションモジュール一覧を取得.
     */
    @GetMapping("/modules")
    public ResponseEntity<List<ApplicationModuleDto>> getApplicationModules() {
        return ResponseEntity.ok(statsService.getApplicationModules());
    }
}
