/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.ContextOverride;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.MessageConfig;

class MiraContextMergeServiceTest {

    private MiraContextMergeService service;
    private MiraContextLayerService contextLayerService;

    @BeforeEach
    void setUp() {
        contextLayerService = mock(MiraContextLayerService.class);
        service = new MiraContextMergeService(contextLayerService);
    }

    @Test
    @DisplayName("Should return base context formatted when no config is provided")
    void shouldReturnBaseContextWhenNoConfig() {
        // Given
        String tenantId = "tenant1";
        String orgId = "org1";
        String userId = "user1";
        Map<String, String> baseContext = new HashMap<>();
        baseContext.put("system", "System instruction");

        when(contextLayerService.buildMergedContext(tenantId, orgId, userId))
                .thenReturn(baseContext);

        // When
        String result = service.buildFinalContextPrompt(tenantId, orgId, userId, null);

        // Then
        assertThat(result).contains("## system");
        assertThat(result).contains("System instruction");
    }

    @Test
    @DisplayName("Should apply overrides to disable a category")
    void shouldApplyOverridesToDisable() {
        // Given
        String tenantId = "tenant1";
        String orgId = "org1";
        String userId = "user1";
        Map<String, String> baseContext = new HashMap<>();
        baseContext.put("system", "System instruction");
        baseContext.put("style", "Style instruction");

        when(contextLayerService.buildMergedContext(tenantId, orgId, userId))
                .thenReturn(baseContext);

        MessageConfig config = MessageConfig.builder()
                .contextOverrides(Map.of("style", ContextOverride.builder().enabled(false).build()))
                .build();

        // When
        String result = service.buildFinalContextPrompt(tenantId, orgId, userId, config);

        // Then
        assertThat(result).contains("## system");
        assertThat(result).doesNotContain("## style");
        assertThat(result).doesNotContain("Style instruction");
    }

    @Test
    @DisplayName("Should append temporary context")
    void shouldAppendTemporaryContext() {
        // Given
        String tenantId = "tenant1";
        String orgId = "org1";
        String userId = "user1";
        Map<String, String> baseContext = new HashMap<>();
        baseContext.put("system", "System instruction");

        when(contextLayerService.buildMergedContext(tenantId, orgId, userId))
                .thenReturn(baseContext);

        MessageConfig config = MessageConfig.builder()
                .temporaryContext("Temporary instruction override")
                .build();

        // When
        String result = service.buildFinalContextPrompt(tenantId, orgId, userId, config);

        // Then
        assertThat(result).contains("## system");
        assertThat(result).contains("## Temporary Context");
        assertThat(result).contains("Temporary instruction override");
    }
}
