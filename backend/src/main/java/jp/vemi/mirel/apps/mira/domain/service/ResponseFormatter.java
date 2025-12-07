/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * レスポンスフォーマッター.
 * 
 * <p>AI応答をクライアント向けに整形します。
 * Markdown変換、コードブロック整形、リンク生成などを行います。</p>
 */
@Slf4j
@Component
public class ResponseFormatter {
    
    /** コードブロックパターン */
    private static final Pattern CODE_BLOCK_PATTERN = 
        Pattern.compile("```(\\w*)\\n([\\s\\S]*?)```", Pattern.MULTILINE);
    
    /** インラインコードパターン */
    private static final Pattern INLINE_CODE_PATTERN = 
        Pattern.compile("`([^`]+)`");
    
    /** URLパターン */
    private static final Pattern URL_PATTERN = 
        Pattern.compile("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)");
    
    /** 見出しパターン */
    private static final Pattern HEADING_PATTERN = 
        Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    
    /**
     * 応答コンテンツ種別.
     */
    public enum ContentType {
        /** Markdown形式 */
        MARKDOWN,
        /** プレーンテキスト */
        PLAIN_TEXT,
        /** HTML形式 */
        HTML
    }
    
    /**
     * AI応答をMarkdown形式で整形.
     *
     * @param rawResponse 生のAI応答
     * @return 整形後のMarkdown
     */
    public String formatAsMarkdown(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return "";
        }
        
        String formatted = rawResponse;
        
        // 余分な空白行を整理
        formatted = formatted.replaceAll("\n{3,}", "\n\n");
        
        // 先頭・末尾の空白を除去
        formatted = formatted.trim();
        
        return formatted;
    }
    
    /**
     * AI応答をプレーンテキストに変換.
     *
     * @param rawResponse 生のAI応答（Markdown含む可能性）
     * @return プレーンテキスト
     */
    public String formatAsPlainText(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return "";
        }
        
        String plain = rawResponse;
        
        // コードブロックを整形
        plain = CODE_BLOCK_PATTERN.matcher(plain).replaceAll("---\n$2\n---");
        
        // インラインコードのバッククォートを除去
        plain = INLINE_CODE_PATTERN.matcher(plain).replaceAll("$1");
        
        // 見出しのマークダウン記号を除去
        plain = HEADING_PATTERN.matcher(plain).replaceAll("$2");
        
        // 強調記号を除去
        plain = plain.replaceAll("\\*\\*(.+?)\\*\\*", "$1"); // bold
        plain = plain.replaceAll("\\*(.+?)\\*", "$1");       // italic
        plain = plain.replaceAll("__(.+?)__", "$1");         // bold
        plain = plain.replaceAll("_(.+?)_", "$1");           // italic
        
        // リスト記号を整理
        plain = plain.replaceAll("^\\s*[-*+]\\s+", "・ ");
        plain = plain.replaceAll("^\\s*\\d+\\.\\s+", "  ");
        
        // 余分な空白行を整理
        plain = plain.replaceAll("\n{3,}", "\n\n");
        
        return plain.trim();
    }
    
    /**
     * AI応答をHTML形式に変換（簡易版）.
     *
     * @param rawResponse 生のAI応答
     * @return HTML
     */
    public String formatAsHtml(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return "";
        }
        
        String html = escapeHtml(rawResponse);
        
        // コードブロック変換
        html = convertCodeBlocksToHtml(html);
        
        // インラインコード変換
        html = INLINE_CODE_PATTERN.matcher(html)
            .replaceAll("<code>$1</code>");
        
        // 見出し変換
        html = convertHeadingsToHtml(html);
        
        // 強調変換
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        
        // URLをリンクに変換
        html = URL_PATTERN.matcher(html)
            .replaceAll("<a href=\"$1\" target=\"_blank\" rel=\"noopener\">$1</a>");
        
        // 改行をbrタグに
        html = html.replace("\n", "<br>\n");
        
        return html;
    }
    
    /**
     * コードブロックを抽出.
     *
     * @param response AI応答
     * @return 抽出されたコードブロックのリスト
     */
    public java.util.List<CodeBlock> extractCodeBlocks(String response) {
        java.util.List<CodeBlock> blocks = new java.util.ArrayList<>();
        
        if (response == null) {
            return blocks;
        }
        
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(response);
        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2).trim();
            blocks.add(new CodeBlock(
                language.isEmpty() ? "text" : language,
                code
            ));
        }
        
        return blocks;
    }
    
    /**
     * 応答の要約を生成（プレビュー用）.
     *
     * @param response AI応答
     * @param maxLength 最大文字数
     * @return 要約テキスト
     */
    public String summarize(String response, int maxLength) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        
        // プレーンテキストに変換
        String plain = formatAsPlainText(response);
        
        // 最初の段落を取得
        String firstParagraph = plain.split("\n\n")[0];
        
        if (firstParagraph.length() <= maxLength) {
            return firstParagraph;
        }
        
        // 文単位で切り取り
        int lastPeriod = firstParagraph.lastIndexOf("。", maxLength);
        if (lastPeriod > 0) {
            return firstParagraph.substring(0, lastPeriod + 1);
        }
        
        // 強制切り取り
        return firstParagraph.substring(0, maxLength - 3) + "...";
    }
    
    // ========================================
    // Private Methods
    // ========================================
    
    private String escapeHtml(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
    
    private String convertCodeBlocksToHtml(String html) {
        Matcher matcher = Pattern.compile("```(\\w*)\\n([\\s\\S]*?)```", Pattern.MULTILINE)
            .matcher(html);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String lang = matcher.group(1);
            String code = matcher.group(2);
            String langClass = lang.isEmpty() ? "" : " class=\"language-" + lang + "\"";
            String replacement = "<pre><code" + langClass + ">" + code + "</code></pre>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private String convertHeadingsToHtml(String html) {
        Matcher matcher = HEADING_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            int level = matcher.group(1).length();
            String text = matcher.group(2);
            String replacement = "<h" + level + ">" + text + "</h" + level + ">";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    // ========================================
    // Inner Classes
    // ========================================
    
    /**
     * コードブロック.
     */
    public record CodeBlock(String language, String code) {}
}
