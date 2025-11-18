/**
 * FreeMarker デコレータ
 * 既存の言語モード（Java/HTML）にFreeMarker構文のハイライトを追加
 */
import { EditorView, Decoration, ViewPlugin, ViewUpdate, DecorationSet } from '@codemirror/view';
import { RangeSetBuilder } from '@codemirror/state';

// FreeMarker構文パターン（プリコンパイル）
const FREEMARKER_PATTERNS = [
  { regex: /\$\{[^}]+\}/g, className: 'cm-freemarker-interpolation' },  // ${variable}
  { regex: /#\{[^}]+\}/g, className: 'cm-freemarker-interpolation' },   // #{variable}
  { regex: /<\/?[#@][a-zA-Z]+[^>]*>/g, className: 'cm-freemarker-directive' }, // <#if>, </@macro>
  { regex: /<#--[\s\S]*?-->/g, className: 'cm-freemarker-comment' },    // <#-- comment -->
];

function createDecorations(view: EditorView): DecorationSet {
  const builder = new RangeSetBuilder<Decoration>();
  const text = view.state.doc.toString();
  const allMatches: Array<{ from: number; to: number; decoration: Decoration }> = [];

  // パターンマッチング
  for (const pattern of FREEMARKER_PATTERNS) {
    const decoration = Decoration.mark({ class: pattern.className });
    
    for (const match of text.matchAll(pattern.regex)) {
      if (match.index !== undefined) {
        allMatches.push({
          from: match.index,
          to: match.index + match[0].length,
          decoration,
        });
      }
    }
  }

  // 位置順にソート
  allMatches.sort((a, b) => a.from - b.from);

  // RangeSetに追加
  for (const { from, to, decoration } of allMatches) {
    builder.add(from, to, decoration);
  }

  return builder.finish();
}

export const freemarkerDecorator = ViewPlugin.fromClass(
  class {
    decorations: DecorationSet;

    constructor(view: EditorView) {
      this.decorations = createDecorations(view);
    }

    update(update: ViewUpdate) {
      if (update.docChanged || update.viewportChanged) {
        this.decorations = createDecorations(update.view);
      }
    }
  },
  {
    decorations: (v) => v.decorations,
  }
);

// CSSスタイル定義
export const freemarkerTheme = EditorView.baseTheme({
  '.cm-freemarker-interpolation': {
    color: '#0066cc',
    fontWeight: 'bold',
  },
  '.cm-freemarker-directive': {
    color: '#cc0066',
    fontWeight: 'bold',
  },
  '.cm-freemarker-comment': {
    color: '#008800',
    fontStyle: 'italic',
  },
});
