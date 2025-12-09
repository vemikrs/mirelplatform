/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * PromptInjectionDetector ReDoS vulnerability verification test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptInjectionReDoSTest {

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraAiProperties.SecurityConfig securityConfig;

    @Mock
    private MiraAiProperties.SecurityConfig.PromptInjectionConfig injectionConfig;

    private PromptInjectionDetector detector;

    @BeforeEach
    void setUp() {
        when(properties.getSecurity()).thenReturn(securityConfig);
        when(securityConfig.getPromptInjection()).thenReturn(injectionConfig);
        when(injectionConfig.isEnabled()).thenReturn(true);
        when(injectionConfig.getSoftBlockThreshold()).thenReturn(3);
        when(injectionConfig.getHardBlockThreshold()).thenReturn(5);

        detector = new PromptInjectionDetector(properties);
    }

    @Test
    @DisplayName("Should not hang on long inputs with 'ignore' and 'forget' repetitions")
    @Timeout(1) // Should complete very quickly
    void testReDoS_IgnoreForget() {
        // Construct a string that triggers potential exponential backtracking in the
        // original regex
        // Pattern:
        // (?i)(ignore|disregard|forget).*(previous|above|prior).*(instructions?|prompt)
        String maliciousInput = "ignore " + "forget ".repeat(10000) + " previous instructions";

        long start = System.currentTimeMillis();
        PromptInjectionDetector.InjectionCheckResult result = detector.check(maliciousInput);
        long end = System.currentTimeMillis();

        assertThat(end - start).as("Execution time should be fast").isLessThan(500);
        // It might be detected or not, but it shouldn't hang
    }

    @Test
    @DisplayName("Should not hang on long inputs with 'repeat the' repetitions")
    @Timeout(1)
    void testReDoS_RepeatThe() {
        // Pattern: (?i)repeat\s+(your|the).*(prompt|instructions)
        String maliciousInput = "repeat the " + "repeat the ".repeat(10000) + " prompt";

        long start = System.currentTimeMillis();
        PromptInjectionDetector.InjectionCheckResult result = detector.check(maliciousInput);
        long end = System.currentTimeMillis();

        assertThat(end - start).as("Execution time should be fast").isLessThan(500);
    }
}
