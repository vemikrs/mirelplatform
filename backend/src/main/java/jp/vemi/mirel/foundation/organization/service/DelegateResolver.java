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
import jp.vemi.mirel.foundation.organization.model.OrganizationUnit;
import jp.vemi.mirel.foundation.organization.model.PositionType;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;
import jp.vemi.mirel.foundation.organization.repository.DelegationRepository;
import jp.vemi.mirel.foundation.organization.repository.OrganizationUnitRepository;
import jp.vemi.mirel.foundation.organization.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DelegateResolver {

    private final DelegationRepository delegationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    /**
     * 代理承認者を解決します.
     * 
     * @param primaryApproverId
     *            本来の承認者ID
     * @param targetDate
     *            対象日
     * @return 代理承認者ID（見つからない場合はEmpty）
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

        OrganizationUnit unit = organizationUnitRepository.findById(primary.getUnitId()).orElse(null);
        if (unit == null) {
            return Optional.empty();
        }

        // 2.1 同組織の副長を探す（簡易実装：役職名に「副」が含まれる、または特定のジョブコードなど）
        // ここでは実装省略

        // 2.2 上位組織の長にエスカレーション
        OrganizationUnit parent = getParentUnit(unit);
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

    private OrganizationUnit getParentUnit(OrganizationUnit unit) {
        if (unit.getParentUnitId() == null) {
            return null;
        }
        return organizationUnitRepository.findById(unit.getParentUnitId()).orElse(null);
    }

    private UserOrganization findManager(OrganizationUnit unit) {
        return userOrganizationRepository.findByUnitId(unit.getUnitId()).stream()
                .filter(uo -> Boolean.TRUE.equals(uo.getIsManager()))
                .findFirst()
                .orElse(null);
    }
}
