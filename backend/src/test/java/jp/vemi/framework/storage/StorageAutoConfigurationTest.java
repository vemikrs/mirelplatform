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

    @Mock
    private StorageProperties.GcsProperties gcsProps;

    @Mock
    private com.google.cloud.storage.Storage storageClient;

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
    void s3StorageService_shouldCreateS3StorageService() {
        // Given
        when(props.getR2()).thenReturn(r2Props);
        when(r2Props.getEndpoint()).thenReturn("https://test.r2.cloudflarestorage.com");
        when(r2Props.getAccessKeyId()).thenReturn("test-key");
        when(r2Props.getSecretAccessKey()).thenReturn("test-secret");
        when(r2Props.getBucket()).thenReturn("test-bucket");
        when(r2Props.getRegion()).thenReturn("auto");
        when(r2Props.getStoragePrefix()).thenReturn("storage/");

        // When
        StorageService service = config.s3StorageService(props);

        // Then
        assertNotNull(service);
        assertInstanceOf(S3StorageService.class, service);
    }

    @Test
    void s3LogStorageService_shouldCreateS3StorageServiceWithLogsPrefix() {
        // Given
        when(props.getR2()).thenReturn(r2Props);
        when(r2Props.getEndpoint()).thenReturn("https://test.r2.cloudflarestorage.com");
        when(r2Props.getAccessKeyId()).thenReturn("test-key");
        when(r2Props.getSecretAccessKey()).thenReturn("test-secret");
        when(r2Props.getBucket()).thenReturn("test-bucket");
        when(r2Props.getRegion()).thenReturn("auto");
        when(r2Props.getLogsPrefix()).thenReturn("logs/");

        // When
        StorageService service = config.s3LogStorageService(props);

        // Then
        assertNotNull(service);
        assertInstanceOf(S3StorageService.class, service);
        assertEquals("logs/", service.getBasePath());
    }

    @Test
    void gcsStorageService_shouldCreateGcsStorageService() {
        // Given
        when(props.getGcs()).thenReturn(gcsProps);
        when(gcsProps.getBucket()).thenReturn("test-gcs-bucket");
        when(gcsProps.getStoragePrefix()).thenReturn("storage/");

        // When
        StorageService service = config.gcsStorageService(props, storageClient);

        // Then
        assertNotNull(service);
        assertInstanceOf(jp.vemi.framework.storage.gcs.GcsStorageService.class, service);
    }

    @Test
    void gcsLogStorageService_shouldCreateGcsLogStorageService() {
        // Given
        when(props.getGcs()).thenReturn(gcsProps);
        when(gcsProps.getBucket()).thenReturn("test-gcs-bucket");

        // When
        StorageService service = config.gcsLogStorageService(props, storageClient);

        // Then
        assertNotNull(service);
        assertInstanceOf(jp.vemi.framework.storage.gcs.GcsStorageService.class, service);
        // デフォルトの logs/ を確認（実装依存）
        assertEquals("logs/", service.getBasePath());
    }
}
