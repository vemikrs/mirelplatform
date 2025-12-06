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
import jp.vemi.mirel.foundation.organization.model.OrganizationUnit;
import jp.vemi.mirel.foundation.organization.model.PositionType;
import jp.vemi.mirel.foundation.organization.model.UnitType;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;
import jp.vemi.mirel.foundation.organization.repository.OrganizationUnitRepository;
import jp.vemi.mirel.foundation.organization.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalRouteResolver {

    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    /**
     * 承認ルートを解決します.
     * 
     * @param applicantUserId
     *            申請者ID
     * @param routeDefinition
     *            ルート定義（カンマ区切りのApproverType）
     * @return 承認ステップのリスト
     */
    @Transactional(readOnly = true)
    public List<ApprovalStep> resolveRoute(String applicantUserId, String routeDefinition) {
        List<ApprovalStep> steps = new ArrayList<>();

        // 1. 申請者の主務組織を取得
        UserOrganization primary = getPrimaryOrganization(applicantUserId);
        if (primary == null) {
            throw new IllegalStateException("Primary organization not found for user: " + applicantUserId);
        }

        OrganizationUnit currentUnit = organizationUnitRepository.findById(primary.getUnitId()).orElse(null);
        if (currentUnit == null) {
            throw new IllegalStateException("Organization unit not found: " + primary.getUnitId());
        }

        // 2. ルート定義をパース
        String[] types = routeDefinition.split(",");

        // 3. 各ステップの承認者を解決
        for (String typeStr : types) {
            ApproverType type = ApproverType.valueOf(typeStr.trim());
            ApprovalStep step = resolveStep(currentUnit, type);

            if (step != null) {
                steps.add(step);
            }

            // 次のステップ用に組織を上に辿る（必要に応じて）
            // 簡易実装として、DEPARTMENT_HEADなどの後は親に移動するロジックを入れるか、
            // あるいはresolveStep内で適切な組織を探すか。
            // ここではresolveStep内で探索し、currentUnitは更新しない（または必要に応じて更新）
            // 設計書では「次のステップ用に組織を上に辿る」とあるが、
            // 連続して承認者を求める場合、例えば「課長→部長」の場合、
            // 課長を見つけた後、その組織の親（部）に移動する必要がある。

            if (shouldMoveToParent(type)) {
                OrganizationUnit parent = getParentUnit(currentUnit);
                if (parent != null) {
                    currentUnit = parent;
                }
            }
        }

        return steps;
    }

    private boolean shouldMoveToParent(ApproverType type) {
        // 承認者タイプに応じて親に移動するか判定
        // 例: 課長承認後は親組織（部）へ
        return type == ApproverType.SECTION_HEAD || type == ApproverType.DEPARTMENT_HEAD;
    }

    private UserOrganization getPrimaryOrganization(String userId) {
        return userOrganizationRepository.findByUserId(userId).stream()
                .filter(uo -> uo.getPositionType() == PositionType.PRIMARY)
                .findFirst()
                .orElse(null);
    }

    private OrganizationUnit getParentUnit(OrganizationUnit unit) {
        if (unit.getParentUnitId() == null) {
            return null;
        }
        return organizationUnitRepository.findById(unit.getParentUnitId()).orElse(null);
    }

    private ApprovalStep resolveStep(OrganizationUnit startUnit, ApproverType type) {
        OrganizationUnit targetUnit = startUnit;
        UserOrganization approver = null;

        switch (type) {
            case DIRECT_MANAGER:
                // 直属上長：現在の組織の長
                approver = findManager(targetUnit);
                break;
            case SECTION_HEAD:
                // 課長：現在の組織から上に辿ってSECTIONを探し、その長
                targetUnit = findUnitByType(startUnit, UnitType.SECTION);
                if (targetUnit != null) {
                    approver = findManager(targetUnit);
                }
                break;
            case DEPARTMENT_HEAD:
                // 部長
                targetUnit = findUnitByType(startUnit, UnitType.DEPARTMENT);
                if (targetUnit != null) {
                    approver = findManager(targetUnit);
                }
                break;
            case DIVISION_HEAD:
                // 本部長
                targetUnit = findUnitByType(startUnit, UnitType.DIVISION);
                if (targetUnit != null) {
                    approver = findManager(targetUnit);
                }
                break;
            default:
                // その他は未実装
                break;
        }

        if (approver != null) {
            ApprovalStep step = new ApprovalStep();
            step.setType(type);
            step.setApproverUserId(approver.getUserId());
            if (targetUnit != null) {
                step.setUnitId(targetUnit.getUnitId());
                step.setUnitName(targetUnit.getName());
            }
            return step;
        }

        return null;
    }

    private OrganizationUnit findUnitByType(OrganizationUnit startUnit, UnitType type) {
        OrganizationUnit current = startUnit;
        while (current != null) {
            if (current.getUnitType() == type) {
                return current;
            }
            current = getParentUnit(current);
        }
        return null;
    }

    private UserOrganization findManager(OrganizationUnit unit) {
        return userOrganizationRepository.findByUnitId(unit.getUnitId()).stream()
                .filter(uo -> Boolean.TRUE.equals(uo.getIsManager()))
                .findFirst()
                .orElse(null);
    }
}
