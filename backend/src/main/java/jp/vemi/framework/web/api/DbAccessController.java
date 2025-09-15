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

import com.google.common.collect.Maps;

/**
 * Minimum Database Access Controller
 * For development and debugging purposes only.
 * Access is restricted to localhost for security.
 */
@RestController
@RequestMapping("/framework/db")
public class DbAccessController {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Execute SELECT query and return results as JSON
     * @param request HTTP request
     * @param body Request body containing SQL query
     * @return Query results
     */
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
            @SuppressWarnings("unchecked")
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