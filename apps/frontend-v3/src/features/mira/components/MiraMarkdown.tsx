/**
 * Mira Markdown Renderer
 * 
 * Markdownコンテンツの安全なレンダリング
 */
import { useMemo } from 'react';
import { cn } from '@mirel/ui';

interface MiraMarkdownProps {
  content: string;
  className?: string;
}

/**
 * 簡易Markdownレンダラー
 * 
 * 対応形式:
 * - 見出し (##, ###)
 * - 強調 (**bold**, *italic*)
 * - コードブロック (```lang)
 * - インラインコード (`code`)
 * - リスト (- item)
 * - リンク ([text](url))
 */
export function MiraMarkdown({ content, className }: MiraMarkdownProps) {
  const rendered = useMemo(() => {
    if (!content) return '';
    
    let html = escapeHtml(content);
    
    // コードブロック
    html = html.replace(
      /```(\w*)\n([\s\S]*?)```/g,
      (_, lang, code) => {
        const langClass = lang ? `language-${lang}` : '';
        return `<pre class="bg-muted rounded-md p-3 overflow-x-auto my-2"><code class="${langClass}">${code.trim()}</code></pre>`;
      }
    );
    
    // インラインコード
    html = html.replace(
      /`([^`]+)`/g,
      '<code class="bg-muted px-1 py-0.5 rounded text-sm">$1</code>'
    );
    
    // 見出し
    html = html.replace(
      /^### (.+)$/gm,
      '<h3 class="text-base font-semibold mt-4 mb-2">$1</h3>'
    );
    html = html.replace(
      /^## (.+)$/gm,
      '<h2 class="text-lg font-semibold mt-4 mb-2">$1</h2>'
    );
    
    // 強調
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    
    // リスト
    html = html.replace(
      /^- (.+)$/gm,
      '<li class="ml-4 list-disc">$1</li>'
    );
    html = html.replace(
      /(<li[^>]*>.*<\/li>\n?)+/g,
      '<ul class="my-2">$&</ul>'
    );
    
    // リンク
    html = html.replace(
      /\[([^\]]+)\]\(([^)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-primary underline">$1</a>'
    );
    
    // 改行
    html = html.replace(/\n/g, '<br />');
    
    return html;
  }, [content]);
  
  return (
    <div 
      className={cn('prose prose-sm dark:prose-invert max-w-none', className)}
      dangerouslySetInnerHTML={{ __html: rendered }}
    />
  );
}

/**
 * HTMLエスケープ
 */
function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  };
  return text.replace(/[&<>"']/g, (m) => map[m]);
}
