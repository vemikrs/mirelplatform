/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.organization;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jp.vemi.mirel.foundation.organization.dto.OrganizationDto;
import jp.vemi.mirel.foundation.organization.dto.UserOrganizationDto;
import jp.vemi.mirel.foundation.organization.service.OrganizationImportService;
import jp.vemi.mirel.foundation.organization.service.OrganizationService;
import jp.vemi.mirel.foundation.organization.service.UserOrganizationService;
import lombok.RequiredArgsConstructor;

/**
 * 組織コントローラー（統合版）.
 */
@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserOrganizationService userOrganizationService;
    private final OrganizationImportService organizationImportService;

    /**
     * 組織一覧を取得.
     */
    @GetMapping
    public List<OrganizationDto> findAll() {
        return organizationService.findAll();
    }

    /**
     * 組織ツリーを取得.
     */
    @GetMapping("/tree")
    public List<OrganizationDto> getTree(@RequestParam String tenantId) {
        return organizationService.getTree(tenantId);
    }

    /**
     * 組織を作成.
     */
    @PostMapping
    public OrganizationDto create(@RequestBody OrganizationDto dto) {
        return organizationService.create(dto);
    }

    /**
     * 祖先組織を取得.
     */
    @GetMapping("/{id}/ancestors")
    public List<OrganizationDto> getAncestors(@PathVariable String id) {
        return organizationService.getAncestors(id);
    }

    /**
     * 子孫組織を取得.
     */
    @GetMapping("/{id}/descendants")
    public List<OrganizationDto> getDescendants(@PathVariable String id) {
        return organizationService.getDescendants(id);
    }

    /**
     * 組織のメンバーを取得.
     */
    @GetMapping("/{id}/members")
    public List<UserOrganizationDto> getMembers(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean includeSubOrgs) {
        return userOrganizationService.findMembers(id, includeSubOrgs);
    }

    /**
     * CSVから組織をインポート.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importOrganizations(
            @RequestParam String tenantId,
            @RequestParam("file") MultipartFile file) throws IOException {
        organizationImportService.importOrganizations(tenantId, file.getInputStream());
    }
}
