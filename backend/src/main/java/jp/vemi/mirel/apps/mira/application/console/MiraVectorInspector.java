package jp.vemi.mirel.apps.mira.application.console;

import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiraVectorInspector implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== MIRA VECTOR INSPECTOR START ===");

        String sql = "SELECT id, metadata->>'fileName' as fileName, content, embedding FROM mir_mira_vector_store LIMIT 5";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.get("id"));
            String fileName = (String) row.get("fileName");
            String content = (String) row.get("content");
            Object embeddingObj = row.get("embedding");

            log.info("ID: {}, File: {}", id, fileName);
            if (content != null) {
                log.info("Content Prefix: {}",
                        content.substring(0, Math.min(content.length(), 100)).replace("\n", " "));
            } else {
                log.warn("Content is NULL");
            }

            if (embeddingObj != null) {
                String vecStr = embeddingObj.toString();
                log.info("Vector Prefix: {}", vecStr.substring(0, Math.min(vecStr.length(), 100)));

                // Check if zero vector
                if (vecStr.contains("[0.0, 0.0, 0.0")) {
                    log.warn("WARNING: Possible Zero Vector detected!");
                }
            } else {
                log.warn("Vector is NULL");
            }
        }

        log.info("=== MIRA VECTOR INSPECTOR END ===");
    }
}
