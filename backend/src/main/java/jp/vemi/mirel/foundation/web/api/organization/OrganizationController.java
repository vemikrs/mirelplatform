/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.organization;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.organization.dto.OrganizationDto;
import jp.vemi.mirel.foundation.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public List<OrganizationDto> findAll() {
        return organizationService.findAll();
    }

    @PostMapping
    public OrganizationDto create(@RequestBody OrganizationDto dto) {
        return organizationService.create(dto);
    }
}
