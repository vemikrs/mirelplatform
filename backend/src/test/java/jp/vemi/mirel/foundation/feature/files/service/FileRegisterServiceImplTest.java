/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.foundation.feature.files.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import jp.vemi.framework.storage.StorageService;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;

/**
 * FileRegisterServiceImpl のテスト。
 */
@ExtendWith(MockitoExtension.class)
class FileRegisterServiceImplTest {

    @Mock
    private FileManagementRepository fileManagementRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileRegisterServiceImpl fileRegisterService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(fileManagementRepository.save(any(FileManagement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void register_multipartFile_savesToStorageService() throws IOException {
        // Given
        byte[] content = "test file content".getBytes();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", content);

        // When
        var result = fileRegisterService.register(multipartFile);

        // Then
        assertNotNull(result);
        assertNotNull(result.getLeft(), "UUID should not be null");

        // Verify storage service was called with byte array
        verify(storageService).saveFile(anyString(), any(byte[].class));
    }

    @Test
    void register_fileWithZip_createsZipArchive() throws IOException {
        // Given
        Path testFile = tempDir.resolve("testdir");
        Files.createDirectories(testFile);
        Path innerFile = testFile.resolve("inner.txt");
        Files.writeString(innerFile, "inner content");

        // When
        var result = fileRegisterService.register(testFile.toFile(), true);

        // Then
        assertNotNull(result);

        // Verify zip was saved
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).saveFile(pathCaptor.capture(), any(byte[].class));

        // Path should exist
        String savedPath = pathCaptor.getValue();
        assertNotNull(savedPath);
    }

    @Test
    void register_singleFile_savesWithOriginalName() throws IOException {
        // Given
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "sample content");

        // When
        var result = fileRegisterService.register(testFile.toFile(), false, "original.txt");

        // Then
        assertNotNull(result);
        assertEquals("original.txt", result.getRight(), "Should preserve original filename");

        // Verify storage service interaction
        verify(storageService).saveFile(anyString(), any(byte[].class));
    }

    @Test
    void register_imageFile_mayCompressImage() throws IOException {
        // Given - create a minimal valid JPEG (too small to actually compress)
        Path testImage = tempDir.resolve("test.jpg");
        // Write minimal bytes that won't cause compression to succeed
        Files.write(testImage, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 });

        // When - this should handle gracefully even if compression fails
        var result = fileRegisterService.register(testImage.toFile(), false, "test.jpg");

        // Then - should still succeed (compression failure returns original data)
        assertNotNull(result);
        verify(storageService).saveFile(anyString(), any(byte[].class));
    }

    @Test
    void register_savesFileManagementEntity() throws IOException {
        // Given
        Path testFile = tempDir.resolve("entity-test.txt");
        Files.writeString(testFile, "entity test");

        // When
        fileRegisterService.register(testFile.toFile(), false, "entity-test.txt");

        // Then
        ArgumentCaptor<FileManagement> entityCaptor = ArgumentCaptor.forClass(FileManagement.class);
        verify(fileManagementRepository).save(entityCaptor.capture());

        FileManagement saved = entityCaptor.getValue();
        assertNotNull(saved);
        assertNotNull(saved.getFileId());
        assertEquals("entity-test.txt", saved.getFileName());
    }
}
