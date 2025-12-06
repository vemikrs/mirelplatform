/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.web.api.admin.dto.ApplicationModuleDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.TenantStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * システム統計サービス.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    /**
     * テナント統計情報を取得.
     */
    @Transactional(readOnly = true)
    public TenantStatsDto getTenantStats() {
        log.debug("Getting tenant stats");
        
        long totalTenants = tenantRepository.count();
        // アクティブテナントは現状すべてアクティブとして扱う（isActive フィールドがあれば使用）
        long activeTenants = totalTenants;
        
        long totalUsers = userRepository.count();
        // アクティブユーザーは isActive = true のユーザー
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .count();
        
        return TenantStatsDto.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .build();
    }

    /**
     * アプリケーションモジュール一覧を取得.
     * 
     * 注: 現在はハードコード。将来的にはDB管理またはプラグインシステムから取得。
     */
    public List<ApplicationModuleDto> getApplicationModules() {
        log.debug("Getting application modules");
        
        List<ApplicationModuleDto> modules = new ArrayList<>();
        
        // ProMarker (コード生成ツール)
        modules.add(ApplicationModuleDto.builder()
                .id("promarker")
                .name("ProMarker")
                .version("1.0.0")
                .status("active")
                .description("コード生成・払出ツール")
                .build());
        
        // Studio (ローコード開発環境)
        modules.add(ApplicationModuleDto.builder()
                .id("studio")
                .name("Studio")
                .version("0.1.0")
                .status("active")
                .description("ローコード開発環境")
                .build());
        
        // 管理機能
        modules.add(ApplicationModuleDto.builder()
                .id("admin")
                .name("管理機能")
                .version("1.0.0")
                .status("active")
                .description("システム管理・ユーザー管理")
                .build());
        
        return modules;
    }
}
