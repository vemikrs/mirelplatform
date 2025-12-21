/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.io.File;
import java.util.List;

import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraKnowledgeDocumentRepository;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira ナレッジベースサービス.
 * <p>
 * ドキュメントの取り込み(Ingestion)と検索(Retrieval)を提供します。
 * System, Tenant, User の3層スコープに対応しています。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraKnowledgeBaseService {

    private final VectorStore vectorStore;
    private final FileManagementRepository fileRepository;
    private final jp.vemi.mirel.apps.mira.domain.dao.repository.MiraKnowledgeDocumentRepository knowledgeDocumentRepository;

    /**
     * ファイルをインデックスに登録します。
     *
     * @param fileId
     *            ファイルID
     * @param scope
     *            スコープ (SYSTEM, TENANT, USER)
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     */
    @Transactional
    public void indexFile(String fileId, MiraVectorStore.Scope scope, String tenantId, String userId) {
        log.info("Indexing file: fileId={}, scope={}, tenantId={}, userId={}", fileId, scope, tenantId, userId);

        FileManagement fileConfig = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (fileConfig.getFilePath() == null) {
            throw new IllegalArgumentException("File path is empty for fileId: " + fileId);
        }

        File file = new File(fileConfig.getFilePath());
        if (!file.exists()) {
            throw new IllegalArgumentException("Physical file not found: " + fileConfig.getFilePath());
        }

        FileSystemResource resource = new FileSystemResource(file) {
            @Override
            public String getFilename() {
                return fileConfig.getFileName();
            }
        };
        log.info("Creating TikaDocumentReader for file: {}, fileName: {}, resource.getFilename: {}",
                file.getAbsolutePath(), fileConfig.getFileName(), resource.getFilename());
        List<Document> documents;
        String fileName = fileConfig.getFileName();
        log.info("Checking manual read for fileName: '{}'", fileName);
        if (fileName != null && fileName.trim().toLowerCase().endsWith(".txt")) {
            log.info("Condition met: fileName ends with .txt");
            try {
                String content = java.nio.file.Files.readString(file.toPath());
                Document doc = new Document(content);
                doc.getMetadata().put("file_name", fileName);
                doc.getMetadata().put("source", fileConfig.getFilePath());
                documents = java.util.Collections.singletonList(doc);
                log.info("Manual text reading successful for file: {}", fileName);
            } catch (Exception e) {
                log.error("Failed to read text file manually, falling back to Tika", e);
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                documents = reader.get();
            }
        } else {
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            documents = reader.get();
        }

        log.info("Reader read {} documents from file {}", documents.size(), fileConfig.getFilePath());
        if (!documents.isEmpty()) {
            log.info("First document content preview: {}",
                    documents.get(0).getText().substring(0, Math.min(100, documents.get(0).getText().length())));
        } else {
            log.warn("Reader returned empty document list for file {}", fileConfig.getFilePath());
        }

        // チャンク分割
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(documents);

        // メタデータ付与
        splitDocuments.forEach(doc -> {
            doc.getMetadata().put("fileId", fileId);
            doc.getMetadata().put("fileName", fileConfig.getFileName());
            doc.getMetadata().put("scope", scope.name());

            if (scope == MiraVectorStore.Scope.SYSTEM) {
                // System scope
            } else if (scope == MiraVectorStore.Scope.TENANT) {
                doc.getMetadata().put("tenantId", tenantId);
            } else if (scope == MiraVectorStore.Scope.USER) {
                doc.getMetadata().put("tenantId", tenantId);
                doc.getMetadata().put("userId", userId);
            }
        });

        // PGVectorへの保存
        vectorStore.add(splitDocuments);

        // 管理エンティティの保存
        jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument doc = knowledgeDocumentRepository
                .findByFileId(fileId)
                .orElse(jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument.builder()
                        .fileId(fileId)
                        .build());
        doc.setScope(scope);
        doc.setTenantId(tenantId);
        doc.setUserId(userId);
        knowledgeDocumentRepository.save(doc);

        log.info("Indexed {} chunks for fileId={}", splitDocuments.size(), fileId);
    }

    /**
     * スコープに基づいてドキュメント一覧を取得します。
     *
     * @param scope
     *            スコープ
     * @param tenantId
     *            テナントID (TENANT/USERスコープ用)
     * @param userId
     *            ユーザーID (USERスコープ用)
     * @return ドキュメントDTOリスト
     */
    @Transactional(readOnly = true)
    public List<jp.vemi.mirel.apps.mira.application.dto.MiraKnowledgeDocumentDto> getDocuments(
            MiraVectorStore.Scope scope, String tenantId, String userId) {

        List<jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument> docs;

        if (scope == MiraVectorStore.Scope.SYSTEM) {
            docs = knowledgeDocumentRepository.findByScope(scope);
        } else if (scope == MiraVectorStore.Scope.TENANT) {
            docs = knowledgeDocumentRepository.findByScopeAndTenantId(scope, tenantId);
        } else {
            docs = knowledgeDocumentRepository.findByScopeAndTenantIdAndUserId(scope, tenantId, userId);
        }

        return docs.stream().map(doc -> {
            FileManagement fm = fileRepository.findById(doc.getFileId()).orElse(null);
            return jp.vemi.mirel.apps.mira.application.dto.MiraKnowledgeDocumentDto.builder()
                    .id(doc.getId())
                    .fileId(doc.getFileId())
                    .fileName(fm != null ? fm.getFileName() : "Unknown")
                    .scope(doc.getScope())
                    .tenantId(doc.getTenantId())
                    .userId(doc.getUserId())
                    .createdAt(doc.getCreatedAt())
                    .updatedAt(doc.getUpdatedAt())
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * ドキュメントを削除します。
     *
     * @param fileId
     *            ファイルID
     */
    @Transactional
    public void deleteDocument(String fileId) {
        // 1. メタデータ削除
        knowledgeDocumentRepository.findByFileId(fileId).ifPresent(knowledgeDocumentRepository::delete);

        // 2. ベクトルストアからの削除 (Spring AI Filter で fileId 指定削除)
        // Note: Spring AI 0.8.1時点では delete(List<String> ids) しか提供されていない場合があるため、
        // フィルタ削除が利用可能か確認が必要。利用できない場合は検索してIDを取得してから削除する。
        // ここでは簡易的に実装するが、大量データの場合は工夫が必要。
        // vectorStore.delete(List.of(fileId)); // これはID指定削除であり、メタデータ指定ではない可能性がある

        // 3. ファイル実体の削除 (FileManagementService経由が望ましいが、ここではRepository直接操作)
        // fileRepository.deleteById(fileId); // 論理削除などを考慮すべき
        fileRepository.findById(fileId).ifPresent(fm -> {
            fm.setDeleteFlag(true);
            fm.setDeleteDate(new java.util.Date());
            fileRepository.save(fm);
        });
    }

    /**
     * ドキュメントの内容を取得します（テキストファイルのみ対応）。
     *
     * @param fileId
     *            ファイルID
     * @return ファイル内容
     */
    @Transactional(readOnly = true)
    public String getDocumentContent(String fileId) {
        FileManagement fileConfig = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        File file = new File(fileConfig.getFilePath());
        if (!file.exists()) {
            throw new IllegalArgumentException("Physical file not found: " + fileConfig.getFilePath());
        }

        try {
            // Use Apache Tika directly to read content as text
            Tika tika = new Tika();
            return tika.parseToString(file);
        } catch (Exception e) {
            log.warn("Failed to read file content with Tika, falling back to plain text read: {}", e.getMessage());
            try {
                return java.nio.file.Files.readString(java.nio.file.Path.of(fileConfig.getFilePath()));
            } catch (java.io.IOException ioe) {
                throw new RuntimeException("Failed to read file content", ioe);
            }
        }
    }

    /**
     * ドキュメントの内容を更新し、再インデックスします。
     *
     * @param fileId
     *            ファイルID
     * @param content
     *            新しい内容
     */
    @Transactional
    public void updateDocumentContent(String fileId, String content) {
        FileManagement fileConfig = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        // ファイル書き込み
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(fileConfig.getFilePath()), content);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write file content", e);
        }

        // 既存のKnowledge情報取得
        jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument doc = knowledgeDocumentRepository
                .findByFileId(fileId)
                .orElseThrow(() -> new IllegalStateException("Knowledge document not found for fileId: " + fileId));

        // 再インデックス
        indexFile(fileId, doc.getScope(), doc.getTenantId(), doc.getUserId());
    }

    /**
     * ユーザーのコンテキストに基づいてドキュメントを検索します。
     * (System + Tenant + User スコープの統合検索)
     *
     * @param query
     *            検索クエリ
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @return 関連ドキュメントリスト
     */
    public List<Document> search(String query, String tenantId, String userId) {
        log.info("RAG Search: query='{}', tenantId={}, userId={}", query, tenantId, userId);

        // Perform separate searches for each scope to ensure reliable retrieval
        // This avoids potential issues with complex nested OR filters in the
        // VectorStore implementation

        List<Document> allDocs = new java.util.ArrayList<>();
        org.springframework.ai.vectorstore.filter.FilterExpressionBuilder b = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
        double threshold = 0.0; // Lower threshold to ensure recall, relying on Top-K to filter relevance

        // 1. System Scope (Accessible to all)
        SearchRequest systemRequest = SearchRequest.builder()
                .query(query)
                .topK(3) // Fetch top 3 from system
                .similarityThreshold(threshold)
                .filterExpression(b.eq("scope", "SYSTEM").build())
                .build();
        List<Document> systemDocs = vectorStore.similaritySearch(systemRequest);
        log.info("RAG Search [SYSTEM]: found {} docs. Threshold={}", systemDocs.size(), threshold);
        allDocs.addAll(systemDocs);

        // 2. Tenant Scope (Accessible to tenant members)
        if (tenantId != null) {
            SearchRequest tenantRequest = SearchRequest.builder()
                    .query(query)
                    .topK(3) // Fetch top 3 from tenant
                    .similarityThreshold(threshold)
                    .filterExpression(b.and(b.eq("scope", "TENANT"), b.eq("tenantId", tenantId)).build())
                    .build();
            List<Document> tenantDocs = vectorStore.similaritySearch(tenantRequest);
            log.info("RAG Search [TENANT]: found {} docs. TenantId={}", tenantDocs.size(), tenantId);
            allDocs.addAll(tenantDocs);
        }

        // 3. User Scope (Accessible to specific user)
        if (userId != null) {
            SearchRequest userRequest = SearchRequest.builder()
                    .query(query)
                    .topK(5) // Prioritize user documents (Fetch top 5)
                    .similarityThreshold(threshold)
                    .filterExpression(b.and(b.eq("scope", "USER"), b.eq("userId", userId)).build())
                    .build();
            List<Document> userDocs = vectorStore.similaritySearch(userRequest);
            log.info("RAG Search [USER]: found {} docs for userId={}", userDocs.size(), userId);
            allDocs.addAll(userDocs);
        }

        // Deduplicate by ID and sort by score (if available) or relevance (assumed by
        // search order)
        // Since we don't have direct access to score in Document object easily without
        // metadata inspection,
        // we'll just return the combined list capped at a reasonable total.
        // Usually VectorStore returns documents with scores, but the Document API hides
        // it in metadata often.

        // Remove duplicates based on ID
        java.util.Set<String> seenIds = new java.util.HashSet<>();
        List<Document> uniqueDocs = new java.util.ArrayList<>();
        for (Document doc : allDocs) {
            if (seenIds.add(doc.getId())) {
                uniqueDocs.add(doc);
            }
        }

        // Limit total results
        return uniqueDocs.subList(0, Math.min(uniqueDocs.size(), 8));
    }

    /**
     * デバッグ用検索（スコア取得）.
     *
     * @param query
     *            検索クエリ
     * @param scope
     *            スコープ
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @param topK
     *            取得数
     * @return ドキュメントリスト
     */
    public List<Document> debugSearch(String query, String scope, String tenantId, String userId, int topK) {
        org.springframework.ai.vectorstore.filter.FilterExpressionBuilder b = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();

        // Scope filter logic (Simulating target user/tenant)
        org.springframework.ai.vectorstore.filter.Filter.Expression expression;

        if ("SYSTEM".equals(scope)) {
            expression = b.eq("scope", "SYSTEM").build();
        } else if ("TENANT".equals(scope)) {
            expression = b.and(b.eq("scope", "TENANT"), b.eq("tenantId", tenantId)).build();
        } else {
            expression = b.and(b.eq("scope", "USER"), b.eq("userId", userId)).build();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(expression)
                // .withSimilarityThreshold(0.0) // Removed as it causes compilation error
                .build();

        return vectorStore.similaritySearch(request);
    }
}
