/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.domain.model.GlobalContext;

/**
 * Mira Markdown Splitter using CommonMark.
 * <p>
 * Splits markdown content by headers and maintains hierarchy context.
 * Phase 2: グローバルコンテキスト注入に対応。
 * </p>
 */
@Component
public class MiraMarkdownSplitter {

    /**
     * ドキュメントを分割します（後方互換性用）。
     */
    public List<Document> apply(List<Document> documents) {
        return apply(documents, null);
    }

    /**
     * ドキュメントを分割し、グローバルコンテキストを各チャンクに注入します。
     *
     * @param documents
     *            分割対象のドキュメント
     * @param globalContext
     *            グローバルコンテキスト（null可）
     * @return 分割されたドキュメントリスト
     */
    public List<Document> apply(List<Document> documents, GlobalContext globalContext) {
        List<Document> splitDocuments = new ArrayList<>();
        Parser parser = Parser.builder().build();

        String globalPrefix = (globalContext != null) ? globalContext.buildPrefix() : "";

        for (Document doc : documents) {
            Node document = parser.parse(doc.getText());
            HeaderVisitor visitor = new HeaderVisitor(doc, globalPrefix);
            document.accept(visitor);
            splitDocuments.addAll(visitor.getChunks());
        }

        return splitDocuments;
    }

    private static class HeaderVisitor extends AbstractVisitor {
        private final Document originalDoc;
        private final String globalPrefix;
        private final List<Document> chunks = new ArrayList<>();
        private final Stack<HeaderInfo> headerStack = new Stack<>();
        private StringBuilder currentContent = new StringBuilder();

        private record HeaderInfo(int level, String text) {
        }

        public HeaderVisitor(Document originalDoc, String globalPrefix) {
            this.originalDoc = originalDoc;
            this.globalPrefix = globalPrefix;
        }

        @Override
        public void visit(Heading heading) {
            // Flush content belonging to the previous section before entering new section
            flushChunk();

            String headingText = extractText(heading);
            int level = heading.getLevel();

            // Update stack: Pop headers that are deeper or same level as current
            while (!headerStack.isEmpty() && headerStack.peek().level >= level) {
                headerStack.pop();
            }
            headerStack.push(new HeaderInfo(level, headingText));

            // Add heading context to content for readability in vector store
            currentContent.append("#".repeat(level)).append(" ").append(headingText).append("\n");

            super.visit(heading);
        }

        @Override
        public void visit(Text text) {
            currentContent.append(text.getLiteral());
            super.visit(text);
        }

        @Override
        public void visit(org.commonmark.node.SoftLineBreak softLineBreak) {
            currentContent.append("\n");
            super.visit(softLineBreak);
        }

        @Override
        public void visit(org.commonmark.node.HardLineBreak hardLineBreak) {
            currentContent.append("\n");
            super.visit(hardLineBreak);
        }

        @Override
        protected void visitChildren(Node parent) {
            super.visitChildren(parent);
            if (parent instanceof org.commonmark.node.Paragraph) {
                currentContent.append("\n\n");
            }
        }

        private void flushChunk() {
            if (currentContent.length() > 0) {
                String text = currentContent.toString().trim();
                // Avoid empty chunks
                if (!text.isEmpty()) {
                    // Build path context
                    String contextPath = headerStack.stream()
                            .map(HeaderInfo::text)
                            .reduce((a, b) -> a + " > " + b)
                            .orElse("");

                    // Inject context at the beginning of the chunk text
                    String contextPrefix = contextPath.isEmpty() ? "" : "[Context: " + contextPath + "]\n";

                    java.util.Map<String, Object> metadata = new java.util.HashMap<>(originalDoc.getMetadata());
                    metadata.put("header_path", contextPath);
                    if (!headerStack.isEmpty()) {
                        metadata.put("current_header", headerStack.peek().text);
                    }

                    // グローバルプレフィックス + コンテキストプレフィックス + 本文
                    String finalText = globalPrefix + contextPrefix + text;
                    chunks.add(new Document(finalText, metadata));
                }
                currentContent.setLength(0);
            }
        }

        private String extractText(Node node) {
            if (node instanceof Text) {
                return ((Text) node).getLiteral();
            }
            StringBuilder sb = new StringBuilder();
            Node child = node.getFirstChild();
            while (child != null) {
                sb.append(extractText(child));
                child = child.getNext();
            }
            return sb.toString();
        }

        public List<Document> getChunks() {
            flushChunk();
            return chunks;
        }
    }
}
