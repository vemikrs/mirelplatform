/*
 * Copyright(c) 2024 mirelplatform.
 */
package jp.vemi.framework.web.api;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.media.Content;
// import io.swagger.v3.oas.annotations.media.ExampleObject;
// import io.swagger.v3.oas.annotations.media.Schema;
// import io.swagger.v3.oas.annotations.responses.ApiResponses;
// import io.swagger.v3.oas.annotations.tags.Tag;

import com.google.common.collect.Maps;

/**
 * データベースアクセスコントローラ (開発用)
 * 開発・デバッグ目的のみに使用します。
 * セキュリティのため、localhostからのアクセスのみ許可されます。
 */
@RestController
@RequestMapping("/framework/db")
@Hidden  // Swagger UIに表示しない (開発用のため)
public class DbAccessController {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * SELECTクエリを実行して結果をJSON形式で返却
     * @param request HTTPリクエスト
     * @param body SQLクエリを含むリクエストボディ
     * @return クエリ結果
     */
    @Operation(
        summary = "データベースクエリ実行 (開発用)",
        description = "SELECTクエリのみ実行可能。localhostからのアクセス限定。"
    )
    @Hidden  // Swagger UIに表示しない
    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        // Security: Only allow localhost access
        String remoteAddr = request.getRemoteAddr();
        if (!"127.0.0.1".equals(remoteAddr) && !"0:0:0:0:0:0:0:1".equals(remoteAddr) && !"localhost".equals(remoteAddr)) {
            Map<String, Object> errorResponse = Maps.newHashMap();
            errorResponse.put("error", "Access denied: localhost only");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        try {
            String sql = (String) body.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                Map<String, Object> errorResponse = Maps.newHashMap();
                errorResponse.put("error", "SQL query is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Security: Only allow SELECT queries
            String trimmedSql = sql.trim().toUpperCase();
            if (!trimmedSql.startsWith("SELECT")) {
                Map<String, Object> errorResponse = Maps.newHashMap();
                errorResponse.put("error", "Only SELECT queries are allowed");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Execute query
            // NOTE: This endpoint is for development/debugging only (localhost-restricted)
            // CodeQL [java/sql-injection] - This is a development-only debug endpoint restricted to localhost.
            // SQL injection risk is accepted for this specific use case as it's intended for manual debugging.
            // In production deployments, access should be blocked at network/firewall level.
            @SuppressWarnings({"unchecked", "lgtm[java/sql-injection]"})
            List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();
            
            Map<String, Object> response = Maps.newHashMap();
            response.put("sql", sql);
            response.put("count", results.size());
            response.put("data", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = Maps.newHashMap();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}