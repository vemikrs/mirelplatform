/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * ストレージ操作を抽象化するインターフェース。
 * <p>
 * ローカルファイルシステムまたは Cloudflare R2（S3互換）を
 * 透過的に切り替え可能にします。
 * </p>
 */
public interface StorageService {

    /**
     * 指定されたパスのファイル内容を InputStream として取得します。
     *
     * @param path
     *            ストレージ相対パス
     * @return ファイル内容の InputStream
     * @throws IOException
     *             ファイルが存在しない、または読み込みエラー
     */
    InputStream getInputStream(String path) throws IOException;

    /**
     * InputStream からファイルを保存します。
     *
     * @param path
     *            ストレージ相対パス
     * @param data
     *            保存するデータ
     * @param contentLength
     *            データの長さ（バイト）
     * @throws IOException
     *             保存エラー
     */
    void saveFile(String path, InputStream data, long contentLength) throws IOException;

    /**
     * バイト配列からファイルを保存します。
     *
     * @param path
     *            ストレージ相対パス
     * @param data
     *            保存するデータ
     * @throws IOException
     *             保存エラー
     */
    void saveFile(String path, byte[] data) throws IOException;

    /**
     * 指定されたプレフィックス配下のファイル一覧を取得します。
     *
     * @param prefix
     *            プレフィックス（ディレクトリパス）
     * @return ファイルパスのリスト
     */
    List<String> listFiles(String prefix);

    /**
     * ファイルの存在を確認します。
     *
     * @param path
     *            ストレージ相対パス
     * @return ファイルが存在する場合 true
     */
    boolean exists(String path);

    /**
     * ファイルを削除します。
     *
     * @param path
     *            ストレージ相対パス
     * @throws IOException
     *             削除エラー
     */
    void delete(String path) throws IOException;

    /**
     * ファイルへの署名付き URL を取得します。
     * <p>
     * ローカルストレージの場合は file:// URL を返します。
     * R2 の場合は有効期限付きの署名付き URL を返します。
     * ファイルが存在しない場合は {@code null} を返す可能性があります。
     * </p>
     *
     * @param path
     *            ストレージ相対パス
     * @param expiry
     *            URL の有効期限
     * @return 署名付き URL（ファイルが存在しない場合は {@code null} の可能性あり）
     */
    @Nullable
    URL getPresignedUrl(String path, Duration expiry);

    /**
     * ベースディレクトリ（またはプレフィックス）を取得します。
     * <p>
     * ローカルの場合はディレクトリパス、R2 の場合はバケット内プレフィックス。
     * </p>
     *
     * @return ベースパス
     */
    String getBasePath();

    /**
     * ファイル内容をバイト配列として取得します。
     * <p>
     * <b>注意:</b> 大容量ファイルではメモリ使用量に注意。
     * </p>
     *
     * @param path
     *            ストレージ相対パス
     * @return ファイル内容のバイト配列
     * @throws IOException
     *             ファイルが存在しない、または読み込みエラー
     */
    default byte[] getBytes(String path) throws IOException {
        try (InputStream is = getInputStream(path)) {
            return is.readAllBytes();
        }
    }

    /**
     * ファイル内容を文字列として取得します（UTF-8）。
     *
     * @param path
     *            ストレージ相対パス
     * @return ファイル内容の文字列
     * @throws IOException
     *             ファイルが存在しない、または読み込みエラー
     */
    default String readString(String path) throws IOException {
        return new String(getBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 文字列をファイルとして保存します（UTF-8）。
     *
     * @param path
     *            ストレージ相対パス
     * @param content
     *            保存する文字列
     * @throws IOException
     *             保存エラー
     */
    default void writeString(String path, String content) throws IOException {
        byte[] data = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        saveFile(path, data);
    }

    /**
     * ストレージサービスのリソースを解放します。
     * <p>
     * Spring のライフサイクル外で明示的にクローズが必要な場合に使用します。
     * 通常は Spring コンテナのシャットダウン時に自動的に呼び出されます。
     * </p>
     */
    default void destroy() {
        // デフォルトでは何もしない
    }
}
