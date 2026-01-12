package jp.vemi.framework.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import jp.vemi.framework.storage.StorageService;

class StorageUtilTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private StorageService storageService;

    private AutoCloseable closeable;
    private final String TEST_DIR = "build/test-storage";

    @BeforeEach
    void setUp() throws IOException, ReflectiveOperationException {
        closeable = MockitoAnnotations.openMocks(this);
        Files.createDirectories(Paths.get(TEST_DIR));
        // Reset StorageUtil state
        StorageUtil.setApplicationContext(null);

        // Inject test directory into StorageConfig via reflection
        setStorageConfigField("initialized", true);
        setStorageConfigField("configuredStorageDir", TEST_DIR);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        StorageUtil.setApplicationContext(null);

        // Reset StorageConfig
        setStorageConfigField("initialized", false);
        setStorageConfigField("configuredStorageDir", null);

        // Cleanup test dir
        deleteDirectory(Paths.get(TEST_DIR).toFile());
    }

    private void setStorageConfigField(String fieldName, Object value) throws ReflectiveOperationException {
        java.lang.reflect.Field field = jp.vemi.framework.config.StorageConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private void deleteDirectory(File start) {
        if (start.exists()) {
            File[] files = start.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            start.delete();
        }
    }

    @Test
    void testGetFile_Fallback() throws IOException {
        // Arrange
        String filename = "test-local.txt";
        Path filePath = Paths.get(TEST_DIR, filename);
        Files.write(filePath, "test content".getBytes());

        // Act
        // No ApplicationContext set, should fallback to local
        File result = StorageUtil.getFile(filename);

        // Assert
        assertNotNull(result);
        assertEquals(filePath.normalize().toFile().getAbsolutePath(), result.getAbsolutePath());
        assertTrue(result.exists());
    }

    @Test
    void testGetFile_WithStorageService_Exists() throws IOException {
        // Arrange
        StorageUtil.setApplicationContext(applicationContext);
        when(applicationContext.getBean(StorageService.class)).thenReturn(storageService);
        when(storageService.exists("remote-file.txt")).thenReturn(true);
        when(storageService.getInputStream("remote-file.txt"))
                .thenReturn(new java.io.ByteArrayInputStream("remote content".getBytes()));

        // Act
        File result = StorageUtil.getFile("remote-file.txt");

        // Assert
        assertNotNull(result);
        assertTrue(result.getName().startsWith("storage-")); // Temp file created
        assertTrue(result.exists());

        // Cleanup temp file
        result.delete();
    }

    @Test
    void testExists_Delegation() {
        // Arrange
        StorageUtil.setApplicationContext(applicationContext);
        when(applicationContext.getBean(StorageService.class)).thenReturn(storageService);
        when(storageService.exists("exists.txt")).thenReturn(true);

        // Act
        boolean result = StorageUtil.exists("exists.txt");

        // Assert
        assertTrue(result);
        verify(storageService).exists("exists.txt");
    }

    @Test
    void testSaveFile_Delegation() throws IOException {
        // Arrange
        StorageUtil.setApplicationContext(applicationContext);
        when(applicationContext.getBean(StorageService.class)).thenReturn(storageService);
        byte[] data = "data".getBytes();

        // Act
        StorageUtil.saveFile("save.txt", data);

        // Assert
        verify(storageService).saveFile("save.txt", data);
    }

    @Test
    void testGetFiles_Fallback() throws IOException {
        // Arrange
        Files.createDirectories(Paths.get(TEST_DIR, "subdir"));
        Files.write(Paths.get(TEST_DIR, "subdir", "f1.txt"), "c1".getBytes());
        Files.write(Paths.get(TEST_DIR, "subdir", "f2.txt"), "c2".getBytes());

        // Act
        List<String> results = StorageUtil.getFiles("subdir");

        // Assert
        assertEquals(2, results.size());
    }
}
