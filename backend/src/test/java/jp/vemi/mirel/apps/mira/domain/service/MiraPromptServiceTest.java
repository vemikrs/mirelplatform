/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.domain.service.MiraPromptService.MiraContext;
import jp.vemi.mirel.apps.mira.domain.service.MiraPromptService.StateContext;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * MiraPromptService のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MiraPromptServiceTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraContextLayerService contextLayerService;

    private ObjectMapper objectMapper;

    private MiraPromptService promptService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        promptService = new MiraPromptService(resourceLoader, objectMapper, properties, contextLayerService);
    }

    @Nested
    @DisplayName("システムプロンプト構築テスト")
    class SystemPromptBuildingTest {

        @Test
        @DisplayName("基本的なシステムプロンプトを構築")
        void shouldBuildBasicSystemPrompt() throws Exception {
            // Arrange
            MiraContext context = MiraContext.builder()
                    .mode(MiraMode.GENERAL_CHAT)
                    .locale("ja")
                    .tenantId("tenant-123")
                    .userId("user-456")
                    .build();
            
            // テンプレートリソースをモック
            setupMockResource("classpath:prompts/identity/mira-identity.md", "# Mira Identity\nYou are Mira.");
            setupMockResource("classpath:prompts/modes/general-chat.md", "# General Chat Mode");
            setupMockResource("classpath:prompts/governance/locale-ja.md", "# Japanese Governance");
            setupMockResource("classpath:prompts/governance/terminology.md", "# Terminology");
            
            when(contextLayerService.buildContextPromptAddition(any(), any(), any()))
                    .thenReturn("");

            // Act
            String result = promptService.buildSystemPrompt(context);

            // Assert
            assertThat(result).contains("Mira Identity");
            assertThat(result).contains("General Chat Mode");
            assertThat(result).contains("Japanese Governance");
        }

        @Test
        @DisplayName("状態コンテキストを含むシステムプロンプト")
        void shouldIncludeStateContext() throws Exception {
            // Arrange
            StateContext stateContext = StateContext.builder()
                    .screenId("/promarker/documents")
                    .selectedEntity("doc-123")
                    .systemRole("admin")
                    .build();
            
            MiraContext context = MiraContext.builder()
                    .mode(MiraMode.CONTEXT_HELP)
                    .locale("ja")
                    .tenantId("tenant-123")
                    .userId("user-456")
                    .screenId("/promarker/documents")
                    .stateContext(stateContext)
                    .build();
            
            setupMockResource("classpath:prompts/identity/mira-identity.md", "# Identity");
            setupMockResource("classpath:prompts/modes/context-help.md", "# Context Help Mode");
            setupMockResource("classpath:prompts/governance/locale-ja.md", "# Japanese");
            setupMockResource("classpath:prompts/governance/terminology.md", "# Terms");
            
            when(contextLayerService.buildContextPromptAddition(any(), any(), any()))
                    .thenReturn("");

            // Act
            String result = promptService.buildSystemPrompt(context);

            // Assert
            assertThat(result).contains("/promarker/documents");
            assertThat(result).contains("doc-123");
        }

        @Test
        @DisplayName("階層コンテキストを含むシステムプロンプト")
        void shouldIncludeHierarchicalContext() throws Exception {
            // Arrange
            MiraContext context = MiraContext.builder()
                    .mode(MiraMode.GENERAL_CHAT)
                    .locale("ja")
                    .tenantId("tenant-123")
                    .organizationId("org-456")
                    .userId("user-789")
                    .build();
            
            setupMockResource("classpath:prompts/identity/mira-identity.md", "# Identity");
            setupMockResource("classpath:prompts/modes/general-chat.md", "# General Chat");
            setupMockResource("classpath:prompts/governance/locale-ja.md", "# Japanese");
            setupMockResource("classpath:prompts/governance/terminology.md", "# Terms");
            
            String contextAddition = "\n## Company Guidelines\n- Follow company policy";
            when(contextLayerService.buildContextPromptAddition(
                    anyString(), anyString(), anyString()))
                    .thenReturn(contextAddition);

            // Act
            String result = promptService.buildSystemPrompt(context);

            // Assert
            assertThat(result).contains("Company Guidelines");
            assertThat(result).contains("Follow company policy");
        }
    }

    @Nested
    @DisplayName("ロケール処理テスト")
    class LocaleProcessingTest {

        @Test
        @DisplayName("英語ロケールを使用")
        void shouldUseEnglishLocale() throws Exception {
            // Arrange
            MiraContext context = MiraContext.builder()
                    .mode(MiraMode.GENERAL_CHAT)
                    .locale("en")
                    .tenantId("tenant-123")
                    .userId("user-456")
                    .build();
            
            setupMockResource("classpath:prompts/identity/mira-identity.md", "# Identity");
            setupMockResource("classpath:prompts/modes/general-chat.md", "# General Chat");
            setupMockResource("classpath:prompts/governance/locale-en.md", "# English Governance");
            setupMockResource("classpath:prompts/governance/terminology.md", "# Terms");
            
            when(contextLayerService.buildContextPromptAddition(any(), any(), any()))
                    .thenReturn("");

            // Act
            String result = promptService.buildSystemPrompt(context);

            // Assert
            assertThat(result).contains("English Governance");
            assertThat(result).contains("Respond in English");
        }

        @Test
        @DisplayName("日本語ロケールを使用")
        void shouldUseJapaneseLocale() throws Exception {
            // Arrange
            MiraContext context = MiraContext.builder()
                    .mode(MiraMode.GENERAL_CHAT)
                    .locale("ja")
                    .tenantId("tenant-123")
                    .userId("user-456")
                    .build();
            
            setupMockResource("classpath:prompts/identity/mira-identity.md", "# Identity");
            setupMockResource("classpath:prompts/modes/general-chat.md", "# General Chat");
            setupMockResource("classpath:prompts/governance/locale-ja.md", "# Japanese Governance");
            setupMockResource("classpath:prompts/governance/terminology.md", "# Terms");
            
            when(contextLayerService.buildContextPromptAddition(any(), any(), any()))
                    .thenReturn("");

            // Act
            String result = promptService.buildSystemPrompt(context);

            // Assert
            assertThat(result).contains("Japanese Governance");
            assertThat(result).contains("Respond in Japanese");
        }
    }

    @Nested
    @DisplayName("モード処理テスト")
    class ModeProcessingTest {

        @Test
        @DisplayName("エラー分析モードを使用")
        void shouldUseErrorAnalyzeMode() throws Exception {
            // Arrange
            MiraContext context = MiraContext.builder()
                    .mode(MiraMode.ERROR_ANALYZE)
                    .locale("ja")
                    .tenantId("tenant-123")
                    .userId("user-456")
                    .build();
            
            setupMockResource("classpath:prompts/identity/mira-identity.md", "# Identity");
            setupMockResource("classpath:prompts/modes/error-analyze.md", "# Error Analysis Mode");
            setupMockResource("classpath:prompts/governance/locale-ja.md", "# Japanese");
            setupMockResource("classpath:prompts/governance/terminology.md", "# Terms");
            
            when(contextLayerService.buildContextPromptAddition(any(), any(), any()))
                    .thenReturn("");

            // Act
            String result = promptService.buildSystemPrompt(context);

            // Assert
            assertThat(result).contains("Error Analysis Mode");
        }

        @Test
        @DisplayName("Studio Agent モードを使用")
        void shouldUseStudioAgentMode() throws Exception {
            // Arrange
            MiraContext context = MiraContext.builder()
                    .mode(MiraMode.STUDIO_AGENT)
                    .locale("ja")
                    .tenantId("tenant-123")
                    .userId("user-456")
                    .build();
            
            setupMockResource("classpath:prompts/identity/mira-identity.md", "# Identity");
            setupMockResource("classpath:prompts/modes/studio-agent.md", "# Studio Agent Mode");
            setupMockResource("classpath:prompts/governance/locale-ja.md", "# Japanese");
            setupMockResource("classpath:prompts/governance/terminology.md", "# Terms");
            
            when(contextLayerService.buildContextPromptAddition(any(), any(), any()))
                    .thenReturn("");

            // Act
            String result = promptService.buildSystemPrompt(context);

            // Assert
            assertThat(result).contains("Studio Agent Mode");
        }
    }

    /**
     * モック用のリソースセットアップヘルパー.
     */
    private void setupMockResource(String location, String content) throws Exception {
        Resource resource = new InMemoryResource(content);
        when(resourceLoader.getResource(location)).thenReturn(resource);
    }

    /**
     * テスト用のインメモリリソース.
     */
    private static class InMemoryResource implements Resource {
        private final String content;

        InMemoryResource(String content) {
            this.content = content;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public java.net.URL getURL() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.net.URI getURI() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.File getFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long contentLength() {
            return content.length();
        }

        @Override
        public long lastModified() {
            return 0;
        }

        @Override
        public Resource createRelative(String relativePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFilename() {
            return "test-resource";
        }

        @Override
        public String getDescription() {
            return "In-memory test resource";
        }
    }
}
