/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.web.api.admin.service.AdminLicenseService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/license")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLicenseController {

    private final AdminLicenseService licenseService;

    @GetMapping
    public ResponseEntity<AdminLicenseService.LicenseSummaryDto> getLicenseInfo() {
        return ResponseEntity.ok(licenseService.getLicenseSummary());
    }

    @PostMapping("/key")
    public ResponseEntity<Void> updateLicenseKey(@RequestBody Map<String, String> body) {
        String key = body.get("licenseKey");
        licenseService.updateLicenseKey(key);
        return ResponseEntity.ok().build();
    }
}
