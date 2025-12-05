/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.feature.tenant.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserTenantRepository;
import jp.vemi.mirel.foundation.feature.tenant.dto.TenantStatus;
import jp.vemi.mirel.foundation.web.api.admin.dto.TenantDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;

    /**
     * 全テナントを取得します.
     *
     * @return テナント一覧
     */
    @Transactional(readOnly = true)
    public List<TenantDto> findAll() {
        return tenantRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * テナントを検索します.
     * 
     * @param query
     *            検索クエリ
     * @return テナント一覧
     */
    /**
     * テナントを検索します.
     * 
     * @param query
     *            検索クエリ
     * @return テナント一覧
     */
    @Transactional(readOnly = true)
    public List<TenantDto> search(String query) {
        if (query == null || query.isEmpty()) {
            return findAll();
        }
        // Simple search logic
        return tenantRepository.findAll().stream()
                .filter(t -> t.getTenantName().contains(query) || t.getDomain().contains(query))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * テナントを作成します.
     */
    public void create(jp.vemi.mirel.foundation.web.api.admin.dto.CreateTenantRequest request) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(java.util.UUID.randomUUID().toString()); // Assuming ID generation
        tenant.setTenantName(request.getTenantName());
        tenant.setDomain(request.getDomain());
        tenant.setPlan(request.getPlan());
        tenant.setStatus(TenantStatus.ACTIVE);

        tenantRepository.save(tenant);

        // TODO: Create Admin User and UserTenant mapping if adminEmail is provided
    }

    /**
     * テナントを更新します.
     */
    public void update(String tenantId, jp.vemi.mirel.foundation.web.api.admin.dto.UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + tenantId));

        if (request.getTenantName() != null) {
            tenant.setTenantName(request.getTenantName());
        }
        if (request.getDomain() != null) {
            tenant.setDomain(request.getDomain());
        }
        if (request.getPlan() != null) {
            tenant.setPlan(request.getPlan());
        }

        tenantRepository.save(tenant);
    }

    /**
     * テナントのステータスを更新します.
     *
     * @param tenantId
     *            テナントID
     * @param status
     *            新しいステータス
     */
    public void updateStatus(String tenantId, TenantStatus status) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found: " + tenantId));
        tenant.setStatus(status);
        tenantRepository.save(tenant);
    }

    private TenantDto toDto(Tenant entity) {
        // TODO: userCountやadminUserは別途集計が必要だが、一旦簡易実装
        Integer userCount = userTenantRepository.countByTenantId(entity.getTenantId());

        // adminUserの取得ロジックは複雑（UserTenantのロールを見る必要がある）ため、
        // 現状は未実装(null)または別途実装。
        // ここでは簡易的に実装
        String adminUser = "system-admin"; // Placeholder

        return TenantDto.builder()
                .tenantId(entity.getTenantId())
                .tenantName(entity.getTenantName())
                .domain(entity.getDomain())
                .plan(entity.getPlan())
                .status(entity.getStatus())
                .adminUser(adminUser)
                .userCount(userCount)
                .createdAt(entity.getCreateDate())
                .build();
    }
}
