/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service;

import jp.vemi.framework.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AvatarService単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AvatarService単体テスト")
class AvatarServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private AvatarService avatarService;

    private UUID userId;
    private String avatarUrl;
    private byte[] mockImageBytes;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        avatarUrl = "https://github.com/avatar.jpg";
        mockImageBytes = "fake-image-data".getBytes();

        // AvatarServiceのcontextPathを設定
        ReflectionTestUtils.setField(avatarService, "contextPath", "/mipla2");

        // RestTemplateを手動で注入（@Mockでは自動注入されない）
        ReflectionTestUtils.setField(avatarService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("画像ダウンロード成功: 正常な画像URLからダウンロード")
    void testDownloadAndSaveAvatar_Success() throws IOException {
        // Given: 正常な画像データ
        when(restTemplate.getForObject(eq(avatarUrl), eq(byte[].class)))
                .thenReturn(mockImageBytes);
        doNothing().when(storageService).saveFile(anyString(), any(byte[].class));

        // When: アバター画像をダウンロード・保存
        String result = avatarService.downloadAndSaveAvatar(avatarUrl, userId);

        // Then: APIエンドポイントURLが返される
        assertThat(result).isEqualTo("/api/users/" + userId + "/avatar");

        // Then: StorageServiceのsaveFileが呼ばれる
        verify(storageService).saveFile(contains(userId.toString()), eq(mockImageBytes));
    }

    @Test
    @DisplayName("画像ダウンロード失敗: 無効なURLまたはネットワークエラー")
    void testDownloadAndSaveAvatar_NetworkError() {
        // Given: ネットワークエラー
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenThrow(new RuntimeException("Network error"));

        // When: アバター画像をダウンロード・保存
        String result = avatarService.downloadAndSaveAvatar(avatarUrl, userId);

        // Then: nullが返される
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("ファイルサイズ超過: 5MBを超える画像の拒否")
    void testDownloadAndSaveAvatar_FileSizeTooLarge() {
        // Given: 5MBを超える画像データ
        byte[] largeImageBytes = new byte[6 * 1024 * 1024]; // 6MB
        when(restTemplate.getForObject(eq(avatarUrl), eq(byte[].class)))
                .thenReturn(largeImageBytes);

        // When: アバター画像をダウンロード・保存
        String result = avatarService.downloadAndSaveAvatar(avatarUrl, userId);

        // Then: nullが返される（サイズ超過）
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("拡張子抽出: URLから正しい拡張子を抽出")
    void testExtractExtension_Jpg() throws IOException {
        // Given: .jpg拡張子のURL
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenReturn(mockImageBytes);
        doNothing().when(storageService).saveFile(anyString(), any(byte[].class));

        // When: アバター画像をダウンロード・保存
        avatarService.downloadAndSaveAvatar("https://example.com/avatar.jpg", userId);

        // Then: .jpg拡張子でsaveFileが呼ばれる
        verify(storageService).saveFile(contains(".jpg"), any(byte[].class));
    }

    @Test
    @DisplayName("拡張子抽出: .png拡張子")
    void testExtractExtension_Png() throws IOException {
        // Given: .png拡張子のURL
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenReturn(mockImageBytes);
        doNothing().when(storageService).saveFile(anyString(), any(byte[].class));

        // When: アバター画像をダウンロード・保存
        avatarService.downloadAndSaveAvatar("https://example.com/avatar.png", userId);

        // Then: .png拡張子でsaveFileが呼ばれる
        verify(storageService).saveFile(contains(".png"), any(byte[].class));
    }

    @Test
    @DisplayName("拡張子抽出: クエリパラメータ付きURL")
    void testExtractExtension_WithQueryParams() throws IOException {
        // Given: クエリパラメータ付きURL
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenReturn(mockImageBytes);
        doNothing().when(storageService).saveFile(anyString(), any(byte[].class));

        // When: アバター画像をダウンロード・保存
        avatarService.downloadAndSaveAvatar("https://example.com/avatar.jpg?size=200", userId);

        // Then: .jpg拡張子でsaveFileが呼ばれる
        verify(storageService).saveFile(contains(".jpg"), any(byte[].class));
    }

    @Test
    @DisplayName("画像取得: 保存済みアバター画像の取得")
    void testGetAvatar_Success() throws IOException {
        // Given: アバター画像がStorageに存在する
        String storagePath = "avatars/" + userId + ".jpg";
        when(storageService.exists(storagePath)).thenReturn(true);
        when(storageService.getBytes(storagePath)).thenReturn(mockImageBytes);

        // When: アバター画像を取得
        byte[] result = avatarService.getAvatar(userId);

        // Then: 画像データが返される
        assertThat(result).isEqualTo(mockImageBytes);
    }

    @Test
    @DisplayName("画像取得: 存在しないアバター画像")
    void testGetAvatar_NotFound() {
        // Given: アバター画像が存在しない
        when(storageService.exists(anyString())).thenReturn(false);

        // When: 存在しないアバター画像を取得
        byte[] result = avatarService.getAvatar(userId);

        // Then: nullが返される
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("画像削除: アバター画像の削除")
    void testDeleteAvatar() throws IOException {
        // Given: アバター画像がStorageに存在する
        String storagePath = "avatars/" + userId + ".jpg";
        when(storageService.exists(storagePath)).thenReturn(true);
        doNothing().when(storageService).delete(storagePath);

        // When: アバター画像を削除
        avatarService.deleteAvatar(userId);

        // Then: StorageServiceのdeleteが呼ばれる
        verify(storageService).delete(storagePath);
    }

    @Test
    @DisplayName("デフォルトアバターURL: 取得")
    void testGetDefaultAvatarUrl() {
        // When: デフォルトアバターURLを取得
        String result = avatarService.getDefaultAvatarUrl();

        // Then: デフォルトURLが返される
        assertThat(result).isEqualTo("/assets/default-avatar.png");
    }

    @Test
    @DisplayName("URLがnullの場合: nullを返す")
    void testDownloadAndSaveAvatar_NullUrl() {
        // When: nullのURLでダウンロード
        String result = avatarService.downloadAndSaveAvatar(null, userId);

        // Then: nullが返される
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("空のURLの場合: nullを返す")
    void testDownloadAndSaveAvatar_EmptyUrl() {
        // When: 空のURLでダウンロード
        String result = avatarService.downloadAndSaveAvatar("", userId);

        // Then: nullが返される
        assertThat(result).isNull();
    }
}
