/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextLayer.ContextScope;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraContextLayerRepository;

/**
 * MiraContextLayerService のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MiraContextLayerServiceTest {

    @Mock
    private MiraContextLayerRepository repository;

    private MiraContextLayerService service;

    private ObjectMapper objectMapper;

    private final String tenantId = "tenant-001";
    private final String organizationId = "org-001";
    private final String userId = "user-001";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new MiraContextLayerService(repository, objectMapper);
    }

    @Nested
    @DisplayName("buildMergedContext テスト")
    class BuildMergedContextTest {

        @Test
        @DisplayName("全スコープのコンテキストをマージできること")
        void shouldMergeAllScopeLayers() {
            // Given
            List<MiraContextLayer> layers = List.of(
                    createLayer(ContextScope.SYSTEM, null, "terminology", "System rules", 100),
                    createLayer(ContextScope.TENANT, tenantId, "terminology", "Tenant rules", 200),
                    createLayer(ContextScope.ORGANIZATION, organizationId, "workflow", "Org workflow", 300),
                    createLayer(ContextScope.USER, userId, "style", "User style", 400)
            );

            when(repository.findAllActiveContextsForUser(eq(tenantId), eq(organizationId), eq(userId)))
                    .thenReturn(layers);

            // When
            Map<String, String> result = service.buildMergedContext(tenantId, organizationId, userId);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get("terminology")).isEqualTo("Tenant rules"); // TENANT が SYSTEM を上書き
            assertThat(result.get("workflow")).isEqualTo("Org workflow");
            assertThat(result.get("style")).isEqualTo("User style");
        }

        @Test
        @DisplayName("同一カテゴリは上位スコープで上書きされること")
        void shouldOverrideByHigherScope() {
            // Given
            List<MiraContextLayer> layers = List.of(
                    createLayer(ContextScope.SYSTEM, null, "setting", "system_value", 100),
                    createLayer(ContextScope.USER, userId, "setting", "user_value", 400)
            );

            when(repository.findAllActiveContextsForUser(eq(tenantId), eq(organizationId), eq(userId)))
                    .thenReturn(layers);

            // When
            Map<String, String> result = service.buildMergedContext(tenantId, organizationId, userId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get("setting")).isEqualTo("user_value");
        }

        @Test
        @DisplayName("コンテキストが空の場合は空のMapを返すこと")
        void shouldReturnEmptyMapWhenNoContexts() {
            // Given
            when(repository.findAllActiveContextsForUser(any(), any(), any()))
                    .thenReturn(List.of());

            // When
            Map<String, String> result = service.buildMergedContext(tenantId, organizationId, userId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMergedContextByCategory テスト")
    class GetMergedContextByCategoryTest {

        @Test
        @DisplayName("特定カテゴリのコンテキストを取得できること")
        void shouldGetContextByCategory() {
            // Given
            List<MiraContextLayer> layers = List.of(
                    createLayer(ContextScope.TENANT, tenantId, "guidelines", "Be helpful", 200)
            );

            when(repository.findAllActiveContextsForUser(eq(tenantId), eq(organizationId), eq(userId)))
                    .thenReturn(layers);

            // When
            String result = service.getMergedContextByCategory(tenantId, organizationId, userId, "guidelines");

            // Then
            assertThat(result).isEqualTo("Be helpful");
        }

        @Test
        @DisplayName("存在しないカテゴリの場合はnullを返すこと")
        void shouldReturnNullForNonexistentCategory() {
            // Given
            when(repository.findAllActiveContextsForUser(any(), any(), any()))
                    .thenReturn(List.of());

            // When
            String result = service.getMergedContextByCategory(tenantId, organizationId, userId, "nonexistent");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getSystemContexts テスト")
    class GetSystemContextsTest {

        @Test
        @DisplayName("システムコンテキストを取得できること")
        void shouldGetSystemContexts() {
            // Given
            List<MiraContextLayer> systemLayers = List.of(
                    createLayer(ContextScope.SYSTEM, null, "global_rules", "Global rules content", 100)
            );

            when(repository.findActiveSystemContexts()).thenReturn(systemLayers);

            // When
            List<MiraContextLayer> result = service.getSystemContexts();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("global_rules");
            verify(repository).findActiveSystemContexts();
        }
    }

    @Nested
    @DisplayName("getTenantContexts テスト")
    class GetTenantContextsTest {

        @Test
        @DisplayName("テナントコンテキストを取得できること")
        void shouldGetTenantContexts() {
            // Given
            List<MiraContextLayer> tenantLayers = List.of(
                    createLayer(ContextScope.TENANT, tenantId, "tenant_rules", "Tenant rules content", 200)
            );

            when(repository.findActiveTenantContexts(eq(tenantId))).thenReturn(tenantLayers);

            // When
            List<MiraContextLayer> result = service.getTenantContexts(tenantId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("tenant_rules");
            verify(repository).findActiveTenantContexts(eq(tenantId));
        }
    }

    @Nested
    @DisplayName("parseContextContent テスト")
    class ParseContextContentTest {

        @Test
        @DisplayName("有効なJSONをMapにパースできること")
        void shouldParseValidJson() {
            // Given
            String json = "{\"key1\":\"value1\",\"key2\":123}";

            // When
            Map<String, Object> result = service.parseContextContent(json);

            // Then
            assertThat(result).containsEntry("key1", "value1");
            assertThat(result).containsEntry("key2", 123);
        }

        @Test
        @DisplayName("無効なJSONの場合はraw値を含むMapを返すこと")
        void shouldReturnRawValueForInvalidJson() {
            // Given
            String invalidJson = "not a json";

            // When
            Map<String, Object> result = service.parseContextContent(invalidJson);

            // Then
            assertThat(result).containsEntry("raw", invalidJson);
        }

        @Test
        @DisplayName("nullの場合は空のMapを返すこと")
        void shouldReturnEmptyMapForNull() {
            // When
            Map<String, Object> result = service.parseContextContent(null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("toJsonContextContent テスト")
    class ToJsonContextContentTest {

        @Test
        @DisplayName("MapをJSON文字列に変換できること")
        void shouldConvertMapToJson() {
            // Given
            Map<String, Object> map = Map.of("key1", "value1", "key2", 123);

            // When
            String result = service.toJsonContextContent(map);

            // Then
            assertThat(result).contains("\"key1\"");
            assertThat(result).contains("\"value1\"");
            assertThat(result).contains("\"key2\"");
            assertThat(result).contains("123");
        }

        @Test
        @DisplayName("nullの場合は空のJSONオブジェクトを返すこと")
        void shouldReturnEmptyJsonForNull() {
            // When
            String result = service.toJsonContextContent(null);

            // Then
            assertThat(result).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("buildContextPromptAddition テスト")
    class BuildContextPromptAdditionTest {

        @Test
        @DisplayName("プロンプト追加用のコンテキストテキストを生成できること")
        void shouldBuildContextPromptAddition() {
            // Given
            List<MiraContextLayer> layers = List.of(
                    createLayer(ContextScope.TENANT, tenantId, "guidelines", "Be helpful", 200)
            );

            when(repository.findAllActiveContextsForUser(eq(tenantId), eq(organizationId), eq(userId)))
                    .thenReturn(layers);

            // When
            String result = service.buildContextPromptAddition(tenantId, organizationId, userId);

            // Then
            assertThat(result).contains("# Additional Context");
            assertThat(result).contains("## guidelines");
            assertThat(result).contains("Be helpful");
        }

        @Test
        @DisplayName("コンテキストが空の場合は空文字列を返すこと")
        void shouldReturnEmptyStringWhenNoContexts() {
            // Given
            when(repository.findAllActiveContextsForUser(any(), any(), any()))
                    .thenReturn(List.of());

            // When
            String result = service.buildContextPromptAddition(tenantId, organizationId, userId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private MiraContextLayer createLayer(ContextScope scope, String scopeId, 
            String category, String content, int priority) {
        return MiraContextLayer.builder()
                .id(UUID.randomUUID().toString())
                .scope(scope)
                .scopeId(scopeId)
                .category(category)
                .content(content)
                .priority(priority)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
