/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;
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
                // System scope has no tenant/user specific constraints usually,
                // but we might want to track who uploaded it?
                // For retrieval filtering, we check scope='SYSTEM'.
            } else if (scope == MiraVectorStore.Scope.TENANT) {
                doc.getMetadata().put("tenantId", tenantId);
            } else if (scope == MiraVectorStore.Scope.USER) {
                doc.getMetadata().put("tenantId", tenantId);
                doc.getMetadata().put("userId", userId);
            }
        });

        // PGVectorへの保存
        // Note: Spring AI の PgVectorStore はデフォルトでは 'vector_store' テーブルを使用します。
        // カスタムテーブル 'mir_mira_vector_store' を使うには VectorStore の Bean 定義時に設定が必要ですが、
        // 今回はシンプルにするため Spring AI のデフォルト動作を前提としつつ、
        // メタデータフィルタリングでスコープ制御を行います。
        // (Entityクラス MiraVectorStore は管理・参照用として機能し、実際のベクトル検索は VectorStore 経由)
        vectorStore.add(splitDocuments);

        log.info("Indexed {} chunks for fileId={}", splitDocuments.size(), fileId);
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
        // フィルタ式構築: (scope == 'SYSTEM') OR (scope == 'TENANT' && tenantId == '...') OR
        // (scope == 'USER' && userId == '...')
        // Spring AI 1.x Filter Expression Syntax:
        // metadata key access might differ. Assuming portable syntax.

        String filterExpression = String.format(
                "(scope == 'SYSTEM') || (scope == 'TENANT' && tenantId == '%s') || (scope == 'USER' && userId == '%s')",
                tenantId, userId);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression(filterExpression)
                .build();

        return vectorStore.similaritySearch(request);
    }
}
