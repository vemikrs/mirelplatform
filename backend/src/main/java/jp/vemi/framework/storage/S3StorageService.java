/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.vemi.framework.util.SanitizeUtil;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import org.springframework.beans.factory.DisposableBean;

/**
 * AWS S3 互換ストレージ (Cloudflare R2 等) 用のストレージサービス実装。
 */
public class S3StorageService implements StorageService, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final String prefix;

    public S3StorageService(StorageProperties.R2Properties r2Props) {
        this(r2Props, r2Props.getStoragePrefix());
    }

    public S3StorageService(StorageProperties.R2Properties r2Props, String prefix) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                r2Props.getAccessKeyId(),
                r2Props.getSecretAccessKey());

        // 認証情報のバリデーション
        if (r2Props.getEndpoint() == null || r2Props.getEndpoint().isBlank()) {
            throw new IllegalArgumentException("R2 endpoint is required but was not configured");
        }
        if (r2Props.getAccessKeyId() == null || r2Props.getAccessKeyId().isBlank()) {
            throw new IllegalArgumentException("R2 access-key-id is required but was not configured");
        }
        if (r2Props.getSecretAccessKey() == null || r2Props.getSecretAccessKey().isBlank()) {
            throw new IllegalArgumentException("R2 secret-access-key is required but was not configured");
        }
        if (r2Props.getBucket() == null || r2Props.getBucket().isBlank()) {
            throw new IllegalArgumentException("R2 bucket is required but was not configured");
        }

        URI endpoint = URI.create(r2Props.getEndpoint());

        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(r2Props.getRegion()))
                .forcePathStyle(true) // R2 requires path-style access
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(r2Props.getRegion()))
                .build();

        this.bucket = r2Props.getBucket();
        this.prefix = normalizePrefix(prefix);

        logger.info("S3StorageService initialized - bucket: {}, prefix: {}", bucket, this.prefix);
    }

    /**
     * テスト用コンストラクタ。
     * S3Client と S3Presigner を直接注入できます。
     */
    S3StorageService(S3Client s3Client, S3Presigner s3Presigner, String bucket, String prefix) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
        logger.info("S3StorageService initialized (test mode) - bucket: {}, prefix: {}", bucket, this.prefix);
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        String p = prefix.replace('\\', '/');
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private String toS3Key(String path) {
        String sanitized = path == null ? "" : path.replace('\\', '/');
        if (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }
        return prefix + sanitized;
    }

    private String fromS3Key(String key) {
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return key;
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        String key = toS3Key(path);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            throw new IOException("File not found: " + path, e);
        } catch (Exception e) {
            throw new IOException("Failed to get file: " + path, e);
        }
    }

    @Override
    public void saveFile(String path, InputStream data, long contentLength) throws IOException {
        String key = toS3Key(path);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentLength(contentLength)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(data, contentLength));
            logger.debug("Saved file to R2: {}", key);
        } catch (Exception e) {
            throw new IOException("Failed to save file: " + path, e);
        }
    }

    @Override
    public void saveFile(String path, byte[] data) throws IOException {
        String key = toS3Key(path);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentLength((long) data.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(data));
            logger.debug("Saved file to R2: {}", SanitizeUtil.forLog(key));
        } catch (Exception e) {
            throw new IOException("Failed to save file: " + path, e);
        }
    }

    @Override
    public List<String> listFiles(String pathPrefix) {
        String fullPrefix = toS3Key(pathPrefix);
        List<String> results = new ArrayList<>();

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(fullPrefix)
                    .build();

            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(request);
                for (S3Object obj : response.contents()) {
                    results.add(fromS3Key(obj.key()));
                }
                request = request.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();
            } while (response.isTruncated());

        } catch (Exception e) {
            logger.error("Failed to list files with prefix: {}", pathPrefix, e);
        }
        return results;
    }

    @Override
    public boolean exists(String path) {
        String key = toS3Key(path);
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Failed to check existence: {}", SanitizeUtil.forLog(path), e);
            return false;
        }
    }

    @Override
    public void delete(String path) throws IOException {
        String key = toS3Key(path);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            logger.debug("Deleted file from R2: {}", key);
        } catch (Exception e) {
            throw new IOException("Failed to delete file: " + path, e);
        }
    }

    @Override
    public URL getPresignedUrl(String path, Duration expiry) {
        String key = toS3Key(path);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url();
        } catch (Exception e) {
            logger.error("Failed to generate presigned URL: {}", path, e);
            return null;
        }
    }

    @Override
    public String getBasePath() {
        return prefix;
    }

    /**
     * Spring Bean ライフサイクルでリソースをクリーンアップします。
     */
    @Override
    public void destroy() {
        logger.info("Closing S3StorageService resources");
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
}
