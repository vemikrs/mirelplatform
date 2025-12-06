/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ResponseFormatter のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class ResponseFormatterTest {

    private ResponseFormatter responseFormatter;

    @BeforeEach
    void setUp() {
        responseFormatter = new ResponseFormatter();
    }

    @Nested
    @DisplayName("formatAsMarkdown メソッドのテスト")
    class FormatAsMarkdownTest {

        @Test
        @DisplayName("正常な応答をフォーマット")
        void shouldFormatNormalResponse() {
            // Arrange
            String input = "これはテスト応答です。";

            // Act
            String result = responseFormatter.formatAsMarkdown(input);

            // Assert
            assertThat(result).isEqualTo("これはテスト応答です。");
        }

        @Test
        @DisplayName("余分な空白行を整理")
        void shouldCleanupExtraBlankLines() {
            // Arrange
            String input = "段落1\n\n\n\n段落2";

            // Act
            String result = responseFormatter.formatAsMarkdown(input);

            // Assert
            assertThat(result).isEqualTo("段落1\n\n段落2");
        }

        @Test
        @DisplayName("先頭・末尾の空白を除去")
        void shouldTrimWhitespace() {
            // Arrange
            String input = "  テスト  \n  ";

            // Act
            String result = responseFormatter.formatAsMarkdown(input);

            // Assert
            assertThat(result).isEqualTo("テスト");
        }

        @Test
        @DisplayName("空文字列は空を返す")
        void shouldReturnEmptyForEmptyInput() {
            // Act
            String result = responseFormatter.formatAsMarkdown("");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("nullは空文字を返す")
        void shouldReturnEmptyForNull() {
            // Act
            String result = responseFormatter.formatAsMarkdown(null);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("formatAsPlainText メソッドのテスト")
    class FormatAsPlainTextTest {

        @Test
        @DisplayName("Markdownをプレーンテキストに変換")
        void shouldConvertMarkdownToPlainText() {
            // Arrange
            String input = "# 見出し\n\n**太字** と *斜体*";

            // Act
            String result = responseFormatter.formatAsPlainText(input);

            // Assert
            assertThat(result).contains("見出し");
            assertThat(result).contains("太字");
            assertThat(result).contains("斜体");
            assertThat(result).doesNotContain("#");
            assertThat(result).doesNotContain("**");
            assertThat(result).doesNotContain("*斜体*");
        }

        @Test
        @DisplayName("コードブロックを整形")
        void shouldFormatCodeBlocks() {
            // Arrange
            String input = "コード例:\n```java\npublic class Test {}\n```";

            // Act
            String result = responseFormatter.formatAsPlainText(input);

            // Assert
            assertThat(result).contains("public class Test");
            assertThat(result).doesNotContain("```");
        }

        @Test
        @DisplayName("インラインコードのバッククォートを除去")
        void shouldRemoveInlineCodeBackticks() {
            // Arrange
            String input = "変数 `count` を使用します";

            // Act
            String result = responseFormatter.formatAsPlainText(input);

            // Assert
            assertThat(result).contains("count");
            assertThat(result).doesNotContain("`");
        }
    }

    @Nested
    @DisplayName("formatAsHtml メソッドのテスト")
    class FormatAsHtmlTest {

        @Test
        @DisplayName("HTMLエスケープを適用")
        void shouldEscapeHtml() {
            // Arrange
            String input = "<script>alert('xss')</script>";

            // Act
            String result = responseFormatter.formatAsHtml(input);

            // Assert
            assertThat(result).doesNotContain("<script>");
            assertThat(result).contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("見出しをHTMLに変換")
        void shouldConvertHeadingsToHtml() {
            // Arrange
            String input = "# 見出し1\n## 見出し2";

            // Act
            String result = responseFormatter.formatAsHtml(input);

            // Assert
            assertThat(result).contains("<h1>");
            assertThat(result).contains("<h2>");
        }

        @Test
        @DisplayName("強調をHTMLに変換")
        void shouldConvertEmphasisToHtml() {
            // Arrange
            String input = "**太字** と *斜体*";

            // Act
            String result = responseFormatter.formatAsHtml(input);

            // Assert
            assertThat(result).contains("<strong>");
            assertThat(result).contains("<em>");
        }

        @Test
        @DisplayName("URLをリンクに変換")
        void shouldConvertUrlsToLinks() {
            // Arrange
            String input = "詳細は https://example.com を参照";

            // Act
            String result = responseFormatter.formatAsHtml(input);

            // Assert
            assertThat(result).contains("<a href=\"https://example.com\"");
            assertThat(result).contains("target=\"_blank\"");
        }
    }

    @Nested
    @DisplayName("extractCodeBlocks メソッドのテスト")
    class ExtractCodeBlocksTest {

        @Test
        @DisplayName("コードブロックを抽出")
        void shouldExtractCodeBlocks() {
            // Arrange
            String input = "例:\n```java\nclass Test {}\n```\n\n```python\ndef func(): pass\n```";

            // Act
            List<ResponseFormatter.CodeBlock> result = responseFormatter.extractCodeBlocks(input);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).language()).isEqualTo("java");
            assertThat(result.get(0).code()).contains("class Test");
            assertThat(result.get(1).language()).isEqualTo("python");
            assertThat(result.get(1).code()).contains("def func");
        }

        @Test
        @DisplayName("言語指定なしのコードブロック")
        void shouldHandleCodeBlockWithoutLanguage() {
            // Arrange
            String input = "```\nplain code\n```";

            // Act
            List<ResponseFormatter.CodeBlock> result = responseFormatter.extractCodeBlocks(input);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).language()).isEqualTo("text");
        }

        @Test
        @DisplayName("コードブロックがない場合は空リスト")
        void shouldReturnEmptyListForNoCodeBlocks() {
            // Act
            List<ResponseFormatter.CodeBlock> result = responseFormatter.extractCodeBlocks("普通のテキスト");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("summarize メソッドのテスト")
    class SummarizeTest {

        @Test
        @DisplayName("短い応答はそのまま返す")
        void shouldReturnShortResponseAsIs() {
            // Act
            String result = responseFormatter.summarize("短い応答です。", 100);

            // Assert
            assertThat(result).isEqualTo("短い応答です。");
        }

        @Test
        @DisplayName("長い応答は切り詰め")
        void shouldTruncateLongResponse() {
            // Arrange
            String longContent = "これは非常に長い応答です。" + "詳細な説明が続きます。".repeat(20);

            // Act
            String result = responseFormatter.summarize(longContent, 50);

            // Assert
            assertThat(result.length()).isLessThanOrEqualTo(53); // 50 + "..."
        }

        @Test
        @DisplayName("文の途中で切らず句点で終わる")
        void shouldCutAtSentenceEnd() {
            // Arrange
            String content = "最初の文です。二番目の文です。三番目の文です。";

            // Act
            String result = responseFormatter.summarize(content, 25);

            // Assert
            // 句点で終わるか、または "..." で終わる
            assertThat(result.endsWith("。") || result.endsWith("...")).isTrue();
        }

        @Test
        @DisplayName("Markdownを除去してから要約")
        void shouldRemoveMarkdownBeforeSummarize() {
            // Arrange
            String content = "# 見出し\n\n**重要な**内容です。詳細が続きます。";

            // Act
            String result = responseFormatter.summarize(content, 30);

            // Assert
            assertThat(result).doesNotContain("#");
            assertThat(result).doesNotContain("**");
        }
    }
}
