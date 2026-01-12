/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * StorageAutoConfiguration のテスト。
 * <p>
 * 各 Bean メソッドが適切な StorageService インスタンスを返すことを検証します。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class StorageAutoConfigurationTest {

    private StorageAutoConfiguration config;

    @Mock
    private StorageProperties props;

    @Mock
    private StorageProperties.LocalProperties localProps;

    @Mock
    private StorageProperties.R2Properties r2Props;

    @BeforeEach
    void setUp() {
        config = new StorageAutoConfiguration();
        when(props.getLocal()).thenReturn(localProps);
        when(localProps.getBaseDir()).thenReturn("./build/test-storage");
    }

    @Test
    void localStorageService_shouldCreateLocalStorageService() {
        // When
        StorageService service = config.localStorageService(props);

        // Then
        assertNotNull(service);
        assertInstanceOf(LocalStorageService.class, service);
    }

    @Test
    void localLogStorageService_shouldCreateLocalStorageServiceWithLogsSubpath() {
        // When
        StorageService service = config.localLogStorageService(props);

        // Then
        assertNotNull(service);
        assertInstanceOf(LocalStorageService.class, service);
        assertTrue(service.getBasePath().contains("logs"));
    }

    @Test
    void r2StorageService_shouldCreateR2StorageService() {
        // Given
        when(props.getR2()).thenReturn(r2Props);
        when(r2Props.getEndpoint()).thenReturn("https://test.r2.cloudflarestorage.com");
        when(r2Props.getAccessKeyId()).thenReturn("test-key");
        when(r2Props.getSecretAccessKey()).thenReturn("test-secret");
        when(r2Props.getBucket()).thenReturn("test-bucket");
        when(r2Props.getRegion()).thenReturn("auto");
        when(r2Props.getStoragePrefix()).thenReturn("storage/");

        // When
        StorageService service = config.r2StorageService(props);

        // Then
        assertNotNull(service);
        assertInstanceOf(R2StorageService.class, service);
    }

    @Test
    void r2LogStorageService_shouldCreateR2StorageServiceWithLogsPrefix() {
        // Given
        when(props.getR2()).thenReturn(r2Props);
        when(r2Props.getEndpoint()).thenReturn("https://test.r2.cloudflarestorage.com");
        when(r2Props.getAccessKeyId()).thenReturn("test-key");
        when(r2Props.getSecretAccessKey()).thenReturn("test-secret");
        when(r2Props.getBucket()).thenReturn("test-bucket");
        when(r2Props.getRegion()).thenReturn("auto");
        when(r2Props.getLogsPrefix()).thenReturn("logs/");

        // When
        StorageService service = config.r2LogStorageService(props);

        // Then
        assertNotNull(service);
        assertInstanceOf(R2StorageService.class, service);
        assertEquals("logs/", service.getBasePath());
    }
}
