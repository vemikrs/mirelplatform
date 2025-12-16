/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ModelCapabilityRegistry のユニットテスト.
 */
class ModelCapabilityRegistryTest {

    private ModelCapabilityRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModelCapabilityRegistry();
    }

    @Test
    @DisplayName("Llama 3.2 Vision should have MULTIMODAL_INPUT capability")
    void testLlama32Vision() {
        Set<ModelCapability> caps = registry.getCapabilities("meta/llama-3.2-11b-vision-instruct");
        assertThat(caps).contains(ModelCapability.MULTIMODAL_INPUT);
        assertThat(caps).contains(ModelCapability.STREAMING);
    }

    @Test
    @DisplayName("Llama 3.2 Vision (short) should have MULTIMODAL_INPUT capability")
    void testLlama32VisionShort() {
        Set<ModelCapability> caps = registry.getCapabilities("llama-3.2-vision");
        assertThat(caps).contains(ModelCapability.MULTIMODAL_INPUT);
        assertThat(caps).contains(ModelCapability.STREAMING);
    }

    @Test
    @DisplayName("Llama 3.2 (Text Only) should NOT have MULTIMODAL_INPUT capability")
    void testLlama32Text() {
        Set<ModelCapability> caps = registry.getCapabilities("meta/llama-3.2-3b-instruct");
        assertThat(caps).doesNotContain(ModelCapability.MULTIMODAL_INPUT);
        assertThat(caps).contains(ModelCapability.STREAMING);
    }

    @Test
    @DisplayName("GPT-4o should have all capabilities")
    void testGpt4o() {
        Set<ModelCapability> caps = registry.getCapabilities("openai/gpt-4o");
        assertThat(caps).contains(
                ModelCapability.TOOL_CALLING,
                ModelCapability.MULTIMODAL_INPUT,
                ModelCapability.STREAMING,
                ModelCapability.JSON_MODE,
                ModelCapability.LONG_CONTEXT,
                ModelCapability.WEB_SEARCH);
    }

    @Test
    @DisplayName("Unknown model should return default STREAMING capability")
    void testUnknownModel() {
        Set<ModelCapability> caps = registry.getCapabilities("unknown-model");
        assertThat(caps).containsExactly(ModelCapability.STREAMING);
    }

    @Test
    @DisplayName("Null or empty model name should return default STREAMING capability")
    void testNullOrEmpty() {
        assertThat(registry.getCapabilities(null)).containsExactly(ModelCapability.STREAMING);
        assertThat(registry.getCapabilities("")).containsExactly(ModelCapability.STREAMING);
    }
}
