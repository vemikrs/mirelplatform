/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraIndexingProgress;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraIndexingProgress.IndexingStatus;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraIndexingProgressRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraKnowledgeDocumentRepository;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jp.vemi.framework.util.SanitizeUtil;

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
    private final MiraKnowledgeDocumentRepository knowledgeDocumentRepository;
    private final MiraSettingService miraSettingService;
    private final MiraMarkdownSplitter markdownSplitter;
    private final MiraMetadataEnricher metadataEnricher;
    private final MiraHybridSearchService hybridSearchService;
    private final MiraQueryTransformService queryTransformService;
    private final jp.vemi.mirel.apps.mira.domain.dao.repository.MiraSearchLogRepository searchLogRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final MiraIndexingProgressRepository indexingProgressRepository;

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
        log.info("Indexing file: fileId={}, scope={}, tenantId={}, userId={}", SanitizeUtil.forLog(fileId), scope,
                SanitizeUtil.forLog(tenantId), SanitizeUtil.forLog(userId));

        // Prevent Duplicates: Delete existing vectors for this fileId before adding new
        // ones
        deleteVectorsByFileId(fileId);

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

        // Use Tika directly to extract text content first, to avoid "Raw Zip/XML"
        // issues with TikaDocumentReader defaults
        Tika tika = new Tika();
        String fileContent;
        try {
            fileContent = tika.parseToString(file);
        } catch (Exception e) {
            log.warn("Tika parse failed, falling back to TikaDocumentReader", e);
            // Fallback to original logic if Tika fails (though unlikely if file exists)
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            documents = reader.get();
            fileContent = null;
        }

        if (fileContent != null) {
            // Create a single document from the extracted text
            Document doc = new Document(fileContent);
            doc.getMetadata().put("source", fileConfig.getFileName());
            documents = java.util.Collections.singletonList(doc);
        } else {
            documents = java.util.Collections.emptyList();
        }

        log.info("Extracted content length: {}", documents.isEmpty() ? 0 : documents.get(0).getText().length());

        // Original logic was:
        // TikaDocumentReader reader = new TikaDocumentReader(resource);
        // documents = reader.get();

        log.info("Reader read {} documents from file {}", documents.size(), fileConfig.getFilePath());
        if (!documents.isEmpty()) {
            log.info("First document content preview: {}",
                    documents.get(0).getText().substring(0, Math.min(100, documents.get(0).getText().length())));
        } else {
            log.warn("Reader returned empty document list for file {}", fileConfig.getFilePath());
        }

        // チャンク分割
        List<Document> splitDocuments;
        String fileName = fileConfig.getFileName().toLowerCase();

        // 既存のナレッジドキュメントからdescriptionを取得（Phase 3: 手動注釈）
        String description = null;
        var existingDoc = knowledgeDocumentRepository.findByFileId(fileId);
        if (existingDoc.isPresent() && existingDoc.get().getDescription() != null) {
            description = existingDoc.get().getDescription();
        }

        // グローバルコンテキストを構築 (Phase 2 & 3)
        jp.vemi.mirel.apps.mira.domain.model.GlobalContext globalContext = jp.vemi.mirel.apps.mira.domain.model.GlobalContext
                .builder()
                .fileName(fileConfig.getFileName())
                .category(determineCategory(fileName))
                .description(description)
                .build();

        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
            log.info("Using MiraMarkdownSplitter for file: {}", fileName);
            splitDocuments = markdownSplitter.apply(documents, globalContext);
        } else {
            // Default token splitter for other formats
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> tokenSplit = splitter.apply(documents);
            // 非Markdownファイルにもグローバルプレフィックスを追加
            splitDocuments = applyGlobalPrefixToDocuments(tokenSplit, globalContext);
        }

        // メタデータ付与 (Enricherへ委譲)
        java.util.Map<String, Object> contextInfo = new java.util.HashMap<>();
        contextInfo.put("fileId", fileId);
        contextInfo.put("fileName", fileConfig.getFileName());
        contextInfo.put("scope", scope.name());

        if (scope == MiraVectorStore.Scope.TENANT) {
            contextInfo.put("tenantId", tenantId);
        } else if (scope == MiraVectorStore.Scope.USER) {
            contextInfo.put("tenantId", tenantId);
            contextInfo.put("userId", userId);
        }

        metadataEnricher.enrich(splitDocuments, contextInfo);

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

        log.info("Indexed {} chunks for fileId={}", splitDocuments.size(), SanitizeUtil.forLog(fileId));
    }

    /**
     * Delete existing vectors for a given fileId to prevent duplicates.
     * Uses JdbcTemplate for efficient deletion based on metadata.
     */
    /**
     * Delete existing vectors for a given fileId to prevent duplicates.
     * Uses JdbcTemplate for efficient deletion based on metadata.
     */
    private void deleteVectorsByFileId(String fileId) {
        String sql = "DELETE FROM mir_mira_vector_store WHERE metadata->>'fileId' = ?";
        int deleted = jdbcTemplate.update(sql, fileId);
        log.info("Deleted {} old vectors for fileId={}", deleted, SanitizeUtil.forLog(fileId));
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
     * 指定されたスコープの文書を再インデックスします。
     * 
     * @param scope
     *            スコープ
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @return 処理結果メッセージ
     */
    public String reindexScope(MiraVectorStore.Scope scope, String tenantId, String userId) {
        List<jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument> docs;

        if (scope == MiraVectorStore.Scope.SYSTEM) {
            docs = knowledgeDocumentRepository.findByScope(scope);
        } else if (scope == MiraVectorStore.Scope.TENANT) {
            docs = knowledgeDocumentRepository.findByScopeAndTenantId(scope, tenantId);
        } else {
            docs = knowledgeDocumentRepository.findByScopeAndTenantIdAndUserId(scope, tenantId, userId);
        }

        log.info("Starting re-index for scope: {}, found {} documents.", scope, docs.size());

        int success = 0;
        int fail = 0;

        for (jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument doc : docs) {
            try {
                indexFile(doc.getFileId(), doc.getScope(), doc.getTenantId(), doc.getUserId());
                success++;
            } catch (Exception e) {
                log.error("Failed to re-index fileId: " + doc.getFileId(), e);
                fail++;
            }
        }

        return String.format("Re-index completed. Success: %d, Fail: %d", success, fail);
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
        log.info("RAG Search: query='{}', tenantId={}, userId={}", SanitizeUtil.forLog(query),
                SanitizeUtil.forLog(tenantId), SanitizeUtil.forLog(userId));

        // Perform separate searches for each scope to ensure reliable retrieval
        // This avoids potential issues with complex nested OR filters in the
        // VectorStore implementation

        List<Document> allDocs = new java.util.ArrayList<>();
        org.springframework.ai.vectorstore.filter.FilterExpressionBuilder b = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();

        // Get threshold from settings (Tenant > System > Properties)
        double threshold = miraSettingService.getVectorSearchThreshold(tenantId);
        int topK = miraSettingService.getVectorSearchTopK(tenantId); // Get configured Top K

        // Transform query (HyDE)
        String hydeQuery = queryTransformService.transformToHypotheticalDocument(query);
        // String hydeQuery = query; // Disable HyDE by default for now to control
        // changes, or enable if confident.
        // Let's use original query for vector search effectively until HyDE is tuned.

        // 1. System Scope (Accessible to all)
        SearchRequest systemRequest = SearchRequest.builder()
                .query(hydeQuery)
                .topK(Math.max(3, topK / 2)) // Heuristic: Fetch fewer system docs unless topK is very small
                .similarityThreshold(threshold)
                .filterExpression(b.eq("scope", "SYSTEM").build())
                .build();
        List<Document> systemDocs = hybridSearchService.search(query, systemRequest, "SYSTEM", null, null);
        log.info("RAG Search [SYSTEM]: found {} docs. Threshold={}", systemDocs.size(), threshold);
        allDocs.addAll(systemDocs);

        // 2. Tenant Scope (Accessible to tenant members)
        if (tenantId != null) {
            SearchRequest tenantRequest = SearchRequest.builder()
                    .query(hydeQuery)
                    .topK(topK) // Use configured Top K
                    .similarityThreshold(threshold)
                    .filterExpression(b.and(b.eq("scope", "TENANT"), b.eq("tenantId", tenantId)).build())
                    .build();
            List<Document> tenantDocs = hybridSearchService.search(query, tenantRequest, "TENANT", tenantId, null);
            log.info("RAG Search [TENANT]: found {} docs. TenantId={}", tenantDocs.size(), tenantId);
            allDocs.addAll(tenantDocs);
        }

        // 3. User Scope (Accessible to specific user)
        if (userId != null) {
            SearchRequest userRequest = SearchRequest.builder()
                    .query(hydeQuery)
                    .topK(topK) // Use configured Top K
                    .similarityThreshold(threshold)
                    .filterExpression(b.and(b.eq("scope", "USER"), b.eq("userId", userId)).build())
                    .build();
            List<Document> userDocs = hybridSearchService.search(query, userRequest, "USER", tenantId, userId);
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

        // Log search with max score
        double maxScore = uniqueDocs.stream().mapToDouble(d -> getScore(d)).max().orElse(0.0);
        saveSearchLog(query, "STANDALONE_RAG", tenantId, userId, maxScore);

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
    public List<Document> debugSearch(String query, String scope, String tenantId, String userId, int topK,
            double threshold) {
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

        // Use HyDE for debug search too? Or raw? Debugger might want raw.
        // Let's use raw for now to debug the index itself, or add a flag later.

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(expression)
                .similarityThreshold(threshold)
                .build();

        List<Document> results = hybridSearchService.search(query, request, scope, tenantId, userId);

        // Log debug search
        double maxScore = results.stream().mapToDouble(d -> getScore(d)).max().orElse(0.0);
        saveSearchLog(query, "DEBUG_RAG", tenantId, userId, maxScore);

        return results;
    }

    private void saveSearchLog(String query, String method, String tenantId, String userId, Double maxScore) {
        try {
            jp.vemi.mirel.apps.mira.domain.dao.entity.MiraSearchLog logEntity = new jp.vemi.mirel.apps.mira.domain.dao.entity.MiraSearchLog();
            logEntity.setQuery(query);
            logEntity.setTenantId(tenantId);
            logEntity.setUserId(userId);
            logEntity.setSearchMethod(method);
            logEntity.setMaxScore(maxScore);
            logEntity.setCreatedAt(java.time.LocalDateTime.now());
            searchLogRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("Failed to save search log", e);
        }
    }

    private Double getScore(Document doc) {
        if (doc.getMetadata().containsKey("rrf_score"))
            return ((Number) doc.getMetadata().get("rrf_score")).doubleValue();
        if (doc.getMetadata().containsKey("score"))
            return ((Number) doc.getMetadata().get("score")).doubleValue();
        if (doc.getMetadata().containsKey("distance"))
            return ((Number) doc.getMetadata().get("distance")).doubleValue();
        return 0.0;
    }

    // ===================================================================================
    // 非同期インデックス処理
    // ===================================================================================

    /**
     * ファイルを非同期でインデックスに登録します。
     *
     * @param fileId
     *            ファイルID
     * @param scope
     *            スコープ
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @return 処理結果のCompletableFuture
     */
    @Async("miraIndexingExecutor")
    public CompletableFuture<String> indexFileAsync(
            String fileId, MiraVectorStore.Scope scope, String tenantId, String userId) {

        log.info("Starting async indexing for fileId={}", fileId);
        updateProgress(fileId, IndexingStatus.PROCESSING, null);

        try {
            indexFile(fileId, scope, tenantId, userId);
            updateProgress(fileId, IndexingStatus.COMPLETED, null);
            log.info("Async indexing completed for fileId={}", fileId);
            return CompletableFuture.completedFuture(fileId);
        } catch (Exception e) {
            log.error("Async indexing failed for fileId={}", fileId, e);
            updateProgress(fileId, IndexingStatus.FAILED, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 指定スコープの全ドキュメントを非同期で再インデックスします。
     *
     * @param scope
     *            スコープ
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @return タスクID
     */
    public String startBulkReindex(MiraVectorStore.Scope scope, String tenantId, String userId) {
        String taskId = java.util.UUID.randomUUID().toString();
        log.info("Starting bulk reindex task: taskId={}, scope={}", taskId, scope);

        // 対象ドキュメント取得
        List<jp.vemi.mirel.apps.mira.domain.dao.entity.MiraKnowledgeDocument> docs;
        if (scope == MiraVectorStore.Scope.SYSTEM) {
            docs = knowledgeDocumentRepository.findByScope(scope);
        } else if (scope == MiraVectorStore.Scope.TENANT) {
            docs = knowledgeDocumentRepository.findByScopeAndTenantId(scope, tenantId);
        } else {
            docs = knowledgeDocumentRepository.findByScopeAndTenantIdAndUserId(scope, tenantId, userId);
        }

        // 各ドキュメントを非同期で再インデックス
        for (var doc : docs) {
            updateProgress(doc.getFileId(), IndexingStatus.PENDING, null);
            indexFileAsync(doc.getFileId(), doc.getScope(), doc.getTenantId(), doc.getUserId());
        }

        return taskId;
    }

    /**
     * インデックス進捗を更新します。
     */
    private void updateProgress(String fileId, IndexingStatus status, String errorMessage) {
        try {
            MiraIndexingProgress progress = indexingProgressRepository.findByFileId(fileId)
                    .orElse(MiraIndexingProgress.builder()
                            .fileId(fileId)
                            .build());
            progress.setStatus(status);
            progress.setErrorMessage(errorMessage);
            indexingProgressRepository.save(progress);
        } catch (Exception e) {
            log.warn("Failed to update indexing progress for fileId={}", fileId, e);
        }
    }

    /**
     * ファイルのインデックス進捗を取得します。
     */
    public MiraIndexingProgress getIndexingProgress(String fileId) {
        return indexingProgressRepository.findByFileId(fileId).orElse(null);
    }

    // ===================================================================================
    // ヘルパーメソッド
    // ===================================================================================

    /**
     * ファイル名からカテゴリを推定します。
     */
    private String determineCategory(String fileName) {
        if (fileName == null)
            return "unknown";

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            return "documentation";
        } else if (lowerName.endsWith(".java") || lowerName.endsWith(".kt") ||
                lowerName.endsWith(".py") || lowerName.endsWith(".js") ||
                lowerName.endsWith(".ts")) {
            return "source_code";
        } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") ||
                lowerName.endsWith(".csv")) {
            return "spreadsheet";
        } else if (lowerName.endsWith(".pdf")) {
            return "document";
        } else if (lowerName.endsWith(".txt")) {
            return "text";
        } else {
            return "general";
        }
    }

    /**
     * 非Markdownドキュメントにグローバルプレフィックスを適用します。
     */
    private List<Document> applyGlobalPrefixToDocuments(
            List<Document> documents,
            jp.vemi.mirel.apps.mira.domain.model.GlobalContext globalContext) {

        if (globalContext == null) {
            return documents;
        }

        String prefix = globalContext.buildPrefix();
        if (prefix.isEmpty()) {
            return documents;
        }

        List<Document> result = new java.util.ArrayList<>();
        for (Document doc : documents) {
            java.util.Map<String, Object> metadata = new java.util.HashMap<>(doc.getMetadata());
            result.add(new Document(prefix + doc.getText(), metadata));
        }
        return result;
    }
}
