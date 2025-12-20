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

        FileSystemResource resource = new FileSystemResource(file);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();

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
        // Filter Expression Construction using Spring AI's FilterExpressionBuilder
        // This approach is safe as it uses parameterized queries through the builder
        // API
        // (b.eq method) rather than string concatenation, preventing injection
        // vulnerabilities.
        //
        // The filter allows documents from:
        // - SYSTEM scope (accessible to all)
        // - TENANT scope matching the user's tenant
        // - USER scope matching the specific user

        org.springframework.ai.vectorstore.filter.FilterExpressionBuilder b = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
        org.springframework.ai.vectorstore.filter.Filter.Expression expression = b.or(
                b.eq("scope", "SYSTEM"),
                b.or(
                        b.and(b.eq("scope", "TENANT"), b.eq("tenantId", tenantId)),
                        b.and(b.eq("scope", "USER"), b.eq("userId", userId))))
                .build();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression(expression)
                .build();

        return vectorStore.similaritySearch(request);
    }
}
