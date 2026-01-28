/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.organization.model.Delegation;
import jp.vemi.mirel.foundation.organization.model.Organization;
import jp.vemi.mirel.foundation.organization.model.PositionType;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;
import jp.vemi.mirel.foundation.organization.repository.DelegationRepository;
import jp.vemi.mirel.foundation.organization.repository.OrganizationRepository;
import jp.vemi.mirel.foundation.organization.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;

/**
 * 代理者解決サービス.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DelegateResolver {

    private final DelegationRepository delegationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * 代理承認者を解決します.
     */
    @Transactional(readOnly = true)
    public Optional<String> findDelegate(String primaryApproverId, LocalDate targetDate) {
        // 1. 明示的な代理設定を確認
        List<Delegation> delegations = delegationRepository.findActiveByPrimaryUserIdAndDate(primaryApproverId,
                targetDate);
        if (!delegations.isEmpty()) {
            return Optional.of(delegations.get(0).getDelegateUserId());
        }

        // 2. 組織階層から代理を自動決定
        UserOrganization primary = getPrimaryOrganization(primaryApproverId);
        if (primary == null) {
            return Optional.empty();
        }

        Organization org = organizationRepository.findById(primary.getOrganizationId()).orElse(null);
        if (org == null) {
            return Optional.empty();
        }

        // 2.2 上位組織の長にエスカレーション
        Organization parent = getParentOrg(org);
        if (parent != null) {
            UserOrganization parentManager = findManager(parent);
            if (parentManager != null) {
                return Optional.of(parentManager.getUserId());
            }
        }

        return Optional.empty();
    }

    private UserOrganization getPrimaryOrganization(String userId) {
        return userOrganizationRepository.findByUserId(userId).stream()
                .filter(uo -> uo.getPositionType() == PositionType.PRIMARY)
                .findFirst()
                .orElse(null);
    }

    private Organization getParentOrg(Organization org) {
        if (org.getParentId() == null) {
            return null;
        }
        return organizationRepository.findById(org.getParentId()).orElse(null);
    }

    private UserOrganization findManager(Organization org) {
        return userOrganizationRepository.findByOrganizationId(org.getId()).stream()
                .filter(UserOrganization::isManager)  // isManager() uses role.equalsIgnoreCase("manager")
                .findFirst()
                .orElse(null);
    }
}
