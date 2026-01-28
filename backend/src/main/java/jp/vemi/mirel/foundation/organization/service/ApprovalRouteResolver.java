/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.organization.dto.ApprovalStep;
import jp.vemi.mirel.foundation.organization.model.ApproverType;
import jp.vemi.mirel.foundation.organization.model.Organization;
import jp.vemi.mirel.foundation.organization.model.OrganizationType;
import jp.vemi.mirel.foundation.organization.model.PositionType;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;
import jp.vemi.mirel.foundation.organization.repository.OrganizationRepository;
import jp.vemi.mirel.foundation.organization.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;

/**
 * 承認ルート解決サービス.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalRouteResolver {

    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * 承認ルートを解決します.
     */
    @Transactional(readOnly = true)
    public List<ApprovalStep> resolveRoute(String applicantUserId, String routeDefinition) {
        List<ApprovalStep> steps = new ArrayList<>();

        // 1. 申請者の主務組織を取得
        UserOrganization primary = getPrimaryOrganization(applicantUserId);
        if (primary == null) {
            throw new IllegalStateException("Primary organization not found for user: " + applicantUserId);
        }

        Organization currentOrg = organizationRepository.findById(primary.getOrganizationId()).orElse(null);
        if (currentOrg == null) {
            throw new IllegalStateException("Organization not found: " + primary.getOrganizationId());
        }

        // 2. ルート定義をパース
        String[] types = routeDefinition.split(",");

        // 3. 各ステップの承認者を解決
        for (String typeStr : types) {
            ApproverType type = ApproverType.valueOf(typeStr.trim());
            ApprovalStep step = resolveStep(currentOrg, type);

            if (step != null) {
                steps.add(step);
            }

            if (shouldMoveToParent(type)) {
                Organization parent = getParentOrg(currentOrg);
                if (parent != null) {
                    currentOrg = parent;
                }
            }
        }

        return steps;
    }

    private boolean shouldMoveToParent(ApproverType type) {
        return type == ApproverType.SECTION_HEAD || type == ApproverType.DEPARTMENT_HEAD;
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

    private ApprovalStep resolveStep(Organization startOrg, ApproverType type) {
        Organization targetOrg = startOrg;
        UserOrganization approver = null;

        switch (type) {
            case DIRECT_MANAGER:
                approver = findManager(targetOrg);
                break;
            case SECTION_HEAD:
                targetOrg = findOrgByType(startOrg, OrganizationType.SECTION);
                if (targetOrg != null) {
                    approver = findManager(targetOrg);
                }
                break;
            case DEPARTMENT_HEAD:
                targetOrg = findOrgByType(startOrg, OrganizationType.DEPARTMENT);
                if (targetOrg != null) {
                    approver = findManager(targetOrg);
                }
                break;
            case DIVISION_HEAD:
                targetOrg = findOrgByType(startOrg, OrganizationType.DIVISION);
                if (targetOrg != null) {
                    approver = findManager(targetOrg);
                }
                break;
            default:
                break;
        }

        if (approver != null) {
            ApprovalStep step = new ApprovalStep();
            step.setType(type);
            step.setApproverUserId(approver.getUserId());
            if (targetOrg != null) {
                step.setOrganizationId(targetOrg.getId());
                step.setOrganizationName(targetOrg.getName());
            }
            return step;
        }

        return null;
    }

    private Organization findOrgByType(Organization startOrg, OrganizationType type) {
        Organization current = startOrg;
        while (current != null) {
            if (current.getType() == type) {
                return current;
            }
            current = getParentOrg(current);
        }
        return null;
    }

    private UserOrganization findManager(Organization org) {
        return userOrganizationRepository.findByOrganizationId(org.getId()).stream()
                .filter(UserOrganization::isManager)  // isManager() uses role.equalsIgnoreCase("manager")
                .findFirst()
                .orElse(null);
    }
}
