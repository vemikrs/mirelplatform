/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.feature.tenant.dto.TenantStatus;
import jp.vemi.mirel.foundation.feature.tenant.service.TenantService;
import jp.vemi.mirel.foundation.web.api.admin.dto.TenantDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class AdminTenantController {

    private final TenantService tenantService;

    @GetMapping
    public List<TenantDto> getTenants() {
        return tenantService.findAll();
    }

    @PostMapping
    public void createTenant(@RequestBody jp.vemi.mirel.foundation.web.api.admin.dto.CreateTenantRequest request) {
        tenantService.create(request);
    }

    @PutMapping("/{tenantId}/status")
    public void updateStatus(@PathVariable String tenantId, @RequestBody Map<String, String> body) {
        TenantStatus status = TenantStatus.valueOf(body.get("status"));
        tenantService.updateStatus(tenantId, status);
    }

    @PutMapping("/{tenantId}")
    public void updateTenant(@PathVariable String tenantId,
            @RequestBody jp.vemi.mirel.foundation.web.api.admin.dto.UpdateTenantRequest request) {
        tenantService.update(tenantId, request);
    }
}
