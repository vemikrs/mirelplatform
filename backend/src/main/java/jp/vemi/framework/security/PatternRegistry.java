/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.framework.security;

import java.util.regex.Pattern;

/**
 * 安全な正規表現パターンレジストリ.
 * <p>
 * ReDoS（正規表現 Denial of Service）攻撃を防ぐため、
 * パターンをpossessive quantifiersで安全化する機能を提供。
 * </p>
 * 
 * <h2>使用例</h2>
 * 
 * <pre>
 * // 通常のパターン（脆弱性あり）
 * Pattern.compile("a{0,100}b");
 * 
 * // 安全なパターン（possessive quantifier自動付加）
 * PatternRegistry.safe("a{0,100}b"); // → "a{0,100}+b"
 * </pre>
 */
public final class PatternRegistry {

    private PatternRegistry() {
        // ユーティリティクラス
    }

    /**
     * 正規表現をpossessive quantifiersで安全化してコンパイル.
     * <p>
     * {@code {n,m}} 形式の量化子を {@code {n,m}+} に変換し、
     * バックトラッキングによる指数関数的計算量を防止。
     * </p>
     *
     * @param regex
     *            正規表現パターン
     * @return 安全化されたコンパイル済みパターン
     */
    public static Pattern safe(String regex) {
        return Pattern.compile(makePossessive(regex));
    }

    /**
     * 正規表現をpossessive quantifiersで安全化してコンパイル（フラグ指定）.
     *
     * @param regex
     *            正規表現パターン
     * @param flags
     *            Pattern.CASE_INSENSITIVE等のフラグ
     * @return 安全化されたコンパイル済みパターン
     */
    public static Pattern safe(String regex, int flags) {
        return Pattern.compile(makePossessive(regex), flags);
    }

    /**
     * 量化子をpossessiveに変換.
     * <p>
     * 変換対象:
     * <ul>
     * <li>{@code {n,m}} → {@code {n,m}+}</li>
     * <li>{@code {n,}} → {@code {n,}+}</li>
     * </ul>
     * 既にpossessive/lazy（+, ?, *）が付いている場合はスキップ。
     * </p>
     *
     * @param regex
     *            元の正規表現
     * @return possessive化された正規表現
     */
    private static String makePossessive(String regex) {
        // {n,m} または {n,} の後に +?* がなければ + を付加
        return regex.replaceAll("\\{(\\d+),(\\d*)\\}(?![+?*])", "{$1,$2}+");
    }

    // ─────────────────────────────────────────────────────────────────
    // 共通パターン定義
    // ─────────────────────────────────────────────────────────────────

    /** 識別子パターン（英数字、ドット、アンダースコア、ハイフン） */
    public static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9._-]+$");

    /** メールアドレスパターン（簡易版） */
    public static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    /** 日本の電話番号パターン */
    public static final Pattern PHONE_JP = Pattern.compile(
            "0[0-9]{1,4}[- ]?[0-9]{1,4}[- ]?[0-9]{4}");

    /** 日本の携帯電話番号パターン */
    public static final Pattern MOBILE_JP = Pattern.compile(
            "0[7-9]0[- ]?[0-9]{4}[- ]?[0-9]{4}");

    /** 郵便番号パターン（日本） */
    public static final Pattern POSTAL_CODE_JP = Pattern.compile(
            "〒?\\d{3}[- ]?\\d{4}");

    /** IPv4アドレスパターン */
    public static final Pattern IPV4 = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
}
