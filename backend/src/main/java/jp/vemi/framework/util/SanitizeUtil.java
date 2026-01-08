package jp.vemi.framework.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 入力サニタイズ用ユーティリティ。
 * - 仕様: "*"（単体ワイルドカード）を許容（仕様変更しない）
 * - それ以外は識別子: [A-Za-z0-9._-]+ のみ許容
 * - 正規名パス（例: "/foo/bar"）は各セグメントに識別子規則を適用し、".." や "\\" を禁止
 */
public final class SanitizeUtil {

    private static final String IDENTIFIER_REGEX = "^[A-Za-z0-9._-]+$";

    private SanitizeUtil() {
    }

    /**
     * 識別子を検証し、"*" を単体で許容する。
     * 
     * @param input
     *            入力
     * @return 入力（そのまま返却）。不正な場合は IllegalArgumentException
     */
    public static String sanitizeIdentifierAllowWildcard(String input) {
        if (StringUtils.isEmpty(input)) {
            return input; // 空は呼び出し側の仕様に委ねる
        }
        if ("*".equals(input)) {
            return input; // ワイルドカード許容
        }
        if (!input.matches(IDENTIFIER_REGEX)) {
            throw new IllegalArgumentException("Invalid identifier: " + input);
        }
        return input;
    }

    /**
     * 正規名パス（先頭が"/"）を検証する。
     * - 先頭は "/" 必須
     * - バックスラッシュ禁止
     * - 各セグメントは空不可、"." ".." 不可
     * - 各セグメントは IDENTIFIER_REGEX に適合
     * 
     * @param canonical
     *            入力パス
     * @return 入力（そのまま返却）。不正な場合は IllegalArgumentException
     */
    public static String sanitizeCanonicalPath(String canonical) {
        if (StringUtils.isEmpty(canonical) || !canonical.startsWith("/")) {
            throw new IllegalArgumentException("Canonical path must start with '/': " + canonical);
        }
        if (canonical.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Backslash is not allowed in canonical path: " + canonical);
        }

        String[] parts = canonical.split("/");
        for (String part : parts) {
            if (StringUtils.isEmpty(part)) {
                // 先頭の"/"により空になる要素はスキップ
                continue;
            }
            if (".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("Dot segments are not allowed in canonical path: " + canonical);
            }
            if (!part.matches(IDENTIFIER_REGEX)) {
                throw new IllegalArgumentException("Invalid path segment: " + part);
            }
        }
        return canonical;
    }

    /**
     * ログ出力用にサニタイズする（Log Injection対策）。
     * 改行文字（CR, LF）をスペースに置換し、制御文字を除去する。
     * 
     * @param input
     *            入力
     * @return サニタイズされた文字列
     */
    public static String forLog(String input) {
        if (input == null) {
            return "null";
        }
        // 改行文字をスペースに置換
        String sanitized = input.replace('\n', ' ').replace('\r', ' ');
        // 制御文字（0x00-0x1F, 0x7F）を除去
        StringBuilder sb = new StringBuilder(sanitized.length());
        for (char c : sanitized.toCharArray()) {
            if (c >= 0x20 && c != 0x7F) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * オブジェクトをログ出力用にサニタイズする（Log Injection対策）。
     * 
     * @param input
     *            入力オブジェクト
     * @return サニタイズされた文字列
     */
    public static String forLog(Object input) {
        if (input == null) {
            return "null";
        }
        return forLog(String.valueOf(input));
    }
}
