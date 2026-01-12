/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalStorageService のユニットテスト。
 */
class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    void saveAndGetFile_withByteArray() throws IOException {
        // Given
        String path = "test/hello.txt";
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        // When
        storageService.saveFile(path, data);

        // Then
        assertTrue(storageService.exists(path));

        try (InputStream is = storageService.getInputStream(path)) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello, World!", content);
        }
    }

    @Test
    void saveAndGetFile_withInputStream() throws IOException {
        // Given
        String path = "test/stream.txt";
        String content = "InputStream Test Content";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(data);

        // When
        storageService.saveFile(path, inputStream, data.length);

        // Then
        assertTrue(storageService.exists(path));

        try (InputStream is = storageService.getInputStream(path)) {
            String readContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(content, readContent);
        }
    }

    @Test
    void listFiles_returnsAllFilesRecursively() throws IOException {
        // Given
        storageService.saveFile("dir1/file1.txt", "content1".getBytes());
        storageService.saveFile("dir1/subdir/file2.txt", "content2".getBytes());
        storageService.saveFile("dir2/file3.txt", "content3".getBytes());

        // When
        List<String> files = storageService.listFiles("dir1");

        // Then
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.contains("file1.txt")));
        assertTrue(files.stream().anyMatch(f -> f.contains("file2.txt")));
    }

    @Test
    void exists_returnsFalseForNonExistentFile() {
        assertFalse(storageService.exists("nonexistent/file.txt"));
    }

    @Test
    void delete_removesFile() throws IOException {
        // Given
        String path = "to-delete.txt";
        storageService.saveFile(path, "delete me".getBytes());
        assertTrue(storageService.exists(path));

        // When
        storageService.delete(path);

        // Then
        assertFalse(storageService.exists(path));
    }

    @Test
    void getPresignedUrl_returnsFileUrl() throws IOException {
        // Given
        String path = "url-test.txt";
        storageService.saveFile(path, "url content".getBytes());

        // When
        var url = storageService.getPresignedUrl(path, Duration.ofMinutes(5));

        // Then
        assertNotNull(url);
        assertTrue(url.toString().startsWith("file:"));
    }

    @Test
    void resolveSecurePath_preventsPathTraversal() {
        // Given
        String maliciousPath = "../../../etc/passwd";

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            storageService.getInputStream(maliciousPath);
        });
    }

    @Test
    void getBasePath_returnsConfiguredBaseDir() {
        assertEquals(tempDir.toAbsolutePath().toString(), storageService.getBasePath());
    }
}
