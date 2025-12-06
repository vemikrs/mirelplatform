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

import jp.vemi.mirel.foundation.web.api.admin.service.AdminSystemSettingsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/system-settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSystemSettingsController {

    private final AdminSystemSettingsService service;

    @GetMapping
    public ResponseEntity<Map<String, String>> getSystemSettings() {
        return ResponseEntity.ok(service.getSystemSettings());
    }

    @PostMapping
    public ResponseEntity<Void> updateSystemSettings(@RequestBody Map<String, String> settings) {
        service.updateSystemSettings(settings);
        return ResponseEntity.ok().build();
    }
}
