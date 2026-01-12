/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * S3StorageService のユニットテスト。
 * Mockito を使用して S3Client をモック。
 */
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String PREFIX = "storage/";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3StorageService(s3Client, s3Presigner, BUCKET_NAME, PREFIX);
    }

    @Test
    void saveFile_withByteArray_callsPutObject() throws IOException {
        // Given
        String path = "test/hello.txt";
        byte[] data = "Hello, R2!".getBytes(StandardCharsets.UTF_8);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        storageService.saveFile(path, data);

        // Then
        verify(s3Client).putObject(
                argThat((PutObjectRequest req) -> req.bucket().equals(BUCKET_NAME) &&
                        req.key().equals(PREFIX + path) &&
                        req.contentLength() == data.length),
                any(RequestBody.class));
    }

    @Test
    void saveFile_withInputStream_callsPutObject() throws IOException {
        // Given
        String path = "test/stream.txt";
        String content = "InputStream Test Content";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(data);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        storageService.saveFile(path, inputStream, data.length);

        // Then
        verify(s3Client).putObject(
                argThat((PutObjectRequest req) -> req.bucket().equals(BUCKET_NAME) &&
                        req.key().equals(PREFIX + path)),
                any(RequestBody.class));
    }

    @Test
    void getInputStream_callsGetObject() throws IOException {
        // Given
        String path = "test/hello.txt";
        byte[] data = "Hello, R2!".getBytes(StandardCharsets.UTF_8);

        GetObjectResponse response = GetObjectResponse.builder().build();

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> new ResponseInputStream<>(response,
                        AbortableInputStream.create(new ByteArrayInputStream(data))));

        // When
        try (InputStream is = storageService.getInputStream(path)) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello, R2!", content);
        }

        // Then
        verify(s3Client).getObject(
                argThat((GetObjectRequest req) -> req.bucket().equals(BUCKET_NAME) &&
                        req.key().equals(PREFIX + path)));
    }

    @Test
    void exists_returnsTrue_whenHeadObjectSucceeds() {
        // Given
        String path = "existing-file.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        // When
        boolean result = storageService.exists(path);

        // Then
        assertTrue(result);
        verify(s3Client).headObject(
                argThat((HeadObjectRequest req) -> req.bucket().equals(BUCKET_NAME) &&
                        req.key().equals(PREFIX + path)));
    }

    @Test
    void exists_returnsFalse_whenNoSuchKeyException() {
        // Given
        String path = "nonexistent-file.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // When
        boolean result = storageService.exists(path);

        // Then
        assertFalse(result);
    }

    @Test
    void delete_callsDeleteObject() throws IOException {
        // Given
        String path = "to-delete.txt";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        // When
        storageService.delete(path);

        // Then
        verify(s3Client).deleteObject(
                argThat((DeleteObjectRequest req) -> req.bucket().equals(BUCKET_NAME) &&
                        req.key().equals(PREFIX + path)));
    }

    @Test
    void listFiles_callsListObjectsV2() {
        // Given
        String pathPrefix = "dir1";
        S3Object file1 = S3Object.builder().key(PREFIX + "dir1/file1.txt").build();
        S3Object file2 = S3Object.builder().key(PREFIX + "dir1/subdir/file2.txt").build();

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Arrays.asList(file1, file2))
                .isTruncated(false)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // When
        List<String> files = storageService.listFiles(pathPrefix);

        // Then
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.contains("file1.txt")));
        assertTrue(files.stream().anyMatch(f -> f.contains("file2.txt")));
    }

    @Test
    void getPresignedUrl_callsPresigner() throws Exception {
        // Given
        String path = "url-test.txt";
        URL expectedUrl = new URL("https://r2.example.com/test-bucket/storage/url-test.txt?sig=xxx");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // When
        URL url = storageService.getPresignedUrl(path, Duration.ofMinutes(5));

        // Then
        assertNotNull(url);
        assertEquals(expectedUrl, url);
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void getBasePath_returnsConfiguredPrefix() {
        assertEquals(PREFIX, storageService.getBasePath());
    }

    @Test
    void toS3Key_normalizesPathWithLeadingSlash() throws IOException {
        // Given
        String path = "/leading-slash.txt";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        storageService.saveFile(path, "test".getBytes());

        // Then - スラッシュが除去されてプレフィックスが付く
        verify(s3Client).putObject(
                argThat((PutObjectRequest req) -> req.key().equals(PREFIX + "leading-slash.txt")),
                any(RequestBody.class));
    }

    @Test
    void getInputStream_throwsIOException_whenNoSuchKey() {
        // Given
        String path = "does-not-exist.txt";
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // When/Then
        assertThrows(IOException.class, () -> {
            storageService.getInputStream(path);
        });
    }
}
