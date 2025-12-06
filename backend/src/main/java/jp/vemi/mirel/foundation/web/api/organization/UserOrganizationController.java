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
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.organization.dto.UserOrganizationDto;
import jp.vemi.mirel.foundation.organization.service.UserOrganizationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/{userId}/organizations")
@RequiredArgsConstructor
public class UserOrganizationController {

    private final UserOrganizationService userOrganizationService;

    @GetMapping
    public List<UserOrganizationDto> findByUserId(@PathVariable String userId) {
        return userOrganizationService.findByUserId(userId);
    }

    @PostMapping
    public UserOrganizationDto assignUser(
            @PathVariable String userId,
            @RequestBody UserOrganizationDto dto) {
        return userOrganizationService.assignUser(userId, dto);
    }
}
