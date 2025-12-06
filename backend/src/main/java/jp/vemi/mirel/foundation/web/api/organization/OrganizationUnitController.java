/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.organization;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.organization.dto.OrganizationUnitDto;
import jp.vemi.mirel.foundation.organization.dto.UserOrganizationDto;
import jp.vemi.mirel.foundation.organization.service.OrganizationUnitService;
import jp.vemi.mirel.foundation.organization.service.UserOrganizationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/organizations/{organizationId}")
@RequiredArgsConstructor
public class OrganizationUnitController {

    private final OrganizationUnitService organizationUnitService;
    private final UserOrganizationService userOrganizationService;
    private final jp.vemi.mirel.foundation.organization.service.OrganizationImportService organizationImportService;

    @GetMapping("/tree")
    public List<OrganizationUnitDto> getTree(@PathVariable String organizationId) {
        return organizationUnitService.getTree(organizationId);
    }

    @PostMapping("/units")
    public OrganizationUnitDto createUnit(
            @PathVariable String organizationId,
            @RequestBody OrganizationUnitDto dto) {
        return organizationUnitService.create(organizationId, dto);
    }

    @GetMapping("/units/{unitId}/ancestors")
    public List<OrganizationUnitDto> getAncestors(
            @PathVariable String organizationId,
            @PathVariable String unitId) {
        return organizationUnitService.getAncestors(unitId);
    }

    @GetMapping("/units/{unitId}/members")
    public List<UserOrganizationDto> getMembers(
            @PathVariable String organizationId,
            @PathVariable String unitId,
            @RequestParam(defaultValue = "false") boolean includeSubUnits) {
        return userOrganizationService.findMembers(unitId, includeSubUnits);
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importUnits(
            @PathVariable String organizationId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        organizationImportService.importUnits(organizationId, file.getInputStream());
    }
}
