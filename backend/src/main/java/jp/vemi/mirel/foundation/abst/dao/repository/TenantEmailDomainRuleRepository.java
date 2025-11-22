/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.TenantEmailDomainRule;
import jp.vemi.mirel.foundation.abst.dao.entity.TenantEmailDomainRule.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TenantEmailDomainRuleリポジトリ.
 */
@Repository
public interface TenantEmailDomainRuleRepository extends JpaRepository<TenantEmailDomainRule, UUID> {
    
    /**
     * テナント別ルール取得
     * 
     * @param tenantId Tenant ID
     * @return ドメインルールリスト
     */
    List<TenantEmailDomainRule> findByTenantId(UUID tenantId);
    
    /**
     * テナント・ドメイン別ルール取得
     * 
     * @param tenantId Tenant ID
     * @param domain メールドメイン
     * @return ドメインルール
     */
    Optional<TenantEmailDomainRule> findByTenantIdAndDomain(UUID tenantId, String domain);
    
    /**
     * テナント・ルールタイプ別ルール取得
     * 
     * @param tenantId Tenant ID
     * @param ruleType ルールタイプ
     * @return ドメインルールリスト
     */
    List<TenantEmailDomainRule> findByTenantIdAndRuleType(UUID tenantId, RuleType ruleType);
}
