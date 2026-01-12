/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage.gcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import jp.vemi.framework.storage.StorageService;

/**
 * Google Cloud Storage (GCS) 用のストレージサービス実装。
 */
public class GcsStorageService implements StorageService {

    private final Storage storage;
    private final String bucketName;
    private final String basePath; // GCSの場合、バケット内のプレフィックスとして扱う

    /**
     * コンストラクタ。
     *
     * @param storage
     *            GCS クライアント
     * @param bucketName
     *            バケット名
     * @param basePath
     *            ベースパス（プレフィックス）
     */
    public GcsStorageService(Storage storage, String bucketName, String basePath) {
        Assert.notNull(storage, "Storage client must not be null");
        Assert.hasText(bucketName, "Bucket name must not be empty");
        this.storage = storage;
        this.bucketName = bucketName;
        // 末尾の / を保証
        if (basePath != null && !basePath.isEmpty() && !basePath.endsWith("/")) {
            this.basePath = basePath + "/";
        } else {
            this.basePath = basePath == null ? "" : basePath;
        }
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        Blob blob = storage.get(BlobId.of(bucketName, resolveKey(path)));
        if (blob == null || !blob.exists()) {
            throw new IOException("File not found: " + path);
        }
        return new ByteArrayInputStream(blob.getContent());
    }

    @Override
    public void saveFile(String path, InputStream data, long contentLength) throws IOException {
        String key = resolveKey(path);
        BlobId blobId = BlobId.of(bucketName, key);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.createFrom(blobInfo, data);
    }

    @Override
    public void saveFile(String path, byte[] data) throws IOException {
        String key = resolveKey(path);
        BlobId blobId = BlobId.of(bucketName, key);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, data);
    }

    @Override
    public List<String> listFiles(String prefix) {
        String resolvePrefix = resolveKey(prefix);
        List<String> files = new ArrayList<>();
        storage.list(bucketName, Storage.BlobListOption.prefix(resolvePrefix))
                .iterateAll()
                .forEach(blob -> files.add(blob.getName()));
        return files;
    }

    @Override
    public boolean exists(String path) {
        Blob blob = storage.get(BlobId.of(bucketName, resolveKey(path)));
        return blob != null && blob.exists();
    }

    @Override
    public void delete(String path) throws IOException {
        storage.delete(BlobId.of(bucketName, resolveKey(path)));
    }

    @Override
    public @Nullable URL getPresignedUrl(String path, Duration expiry) {
        String key = resolveKey(path);
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, key)).build();

        // Workload Identity または Service Account Key があれば署名可能
        // 注: GCSの signUrl は内部で秘密鍵を使用するため、認証方法によっては機能しない場合がある(IAM SignBlob権限が必要)。
        // 開発環境(User Credentials)では機能しないことが多い。
        try {
            return storage.signUrl(blobInfo, expiry.toSeconds(), TimeUnit.SECONDS,
                    SignUrlOption.withV4Signature());
        } catch (Exception e) {
            // 署名に失敗した場合は null を返すか、例外ハンドリングはお任せ
            // ここではログを出して null を返す設計とする
            // (e.printStackTrace() は避けるが、簡単のため)
            return null;
        }
    }

    @Override
    public String getBasePath() {
        return basePath;
    }

    private String resolveKey(String path) {
        if (basePath.isEmpty()) {
            return path;
        }
        // パスの結合ロジック
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return basePath + path;
    }
}
