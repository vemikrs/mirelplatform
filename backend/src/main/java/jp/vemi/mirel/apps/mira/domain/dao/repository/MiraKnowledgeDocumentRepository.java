/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;

@Repository
public interface MiraKnowledgeDocumentRepository extends JpaRepository<MiraKnowledgeDocument, String> {

    List<MiraKnowledgeDocument> findByScope(MiraVectorStore.Scope scope);

    List<MiraKnowledgeDocument> findByScopeAndTenantId(MiraVectorStore.Scope scope, String tenantId);

    List<MiraKnowledgeDocument> findByScopeAndTenantIdAndUserId(MiraVectorStore.Scope scope, String tenantId,
            String userId);

    Optional<MiraKnowledgeDocument> findByFileId(String fileId);
}
