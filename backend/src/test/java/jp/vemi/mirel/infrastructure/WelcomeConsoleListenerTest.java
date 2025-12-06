/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * WelcomeConsoleListener のテスト.
 */
@ExtendWith(MockitoExtension.class)
class WelcomeConsoleListenerTest {

    @Mock
    private Environment environment;

    private WelcomeConsoleListener listener;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listener = new WelcomeConsoleListener(environment);
        
        // フィールドを設定
        ReflectionTestUtils.setField(listener, "appName", "mirelplatform");
        ReflectionTestUtils.setField(listener, "appVersion", "1.0.0-TEST");
        ReflectionTestUtils.setField(listener, "contextPath", "/mipla2");
        ReflectionTestUtils.setField(listener, "serverPort", "3000");

        // ログキャプチャの設定
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(WelcomeConsoleListener.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void testOnApplicationReadyWithActiveProfiles() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev", "test"});
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        // When
        listener.onApplicationReady(event);

        // Then
        assertTrue(listAppender.list.size() >= 2, "Should log welcome message and READY token");
        
        String logMessage = listAppender.list.get(0).getFormattedMessage();
        assertTrue(logMessage.contains("mirelplatform"), "Should contain app name");
        assertTrue(logMessage.contains("1.0.0-TEST"), "Should contain version");
        assertTrue(logMessage.contains("3000"), "Should contain port");
        assertTrue(logMessage.contains("dev, test"), "Should contain active profiles");
        assertTrue(logMessage.contains("/mipla2"), "Should contain context path");
        assertTrue(logMessage.contains("READY"), "Should contain READY status");
        
        String readyToken = listAppender.list.get(1).getFormattedMessage();
        assertTrue(readyToken.contains("READY"), "Should log READY token");
    }

    @Test
    void testOnApplicationReadyWithoutActiveProfiles() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        // When
        listener.onApplicationReady(event);

        // Then
        assertTrue(listAppender.list.size() >= 2, "Should log welcome message and READY token");
        
        String logMessage = listAppender.list.get(0).getFormattedMessage();
        assertTrue(logMessage.contains("default"), "Should contain 'default' when no active profiles");
    }

    @Test
    void testWelcomeMessageContainsAsciiArt() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        // When
        listener.onApplicationReady(event);

        // Then
        String logMessage = listAppender.list.get(0).getFormattedMessage();
        assertTrue(logMessage.contains("mirel"), "ASCII art should contain 'mirel'");
        assertTrue(logMessage.contains("platform"), "ASCII art should contain 'platform'");
        assertTrue(logMessage.contains("####"), "ASCII art should contain hash characters");
    }
}
