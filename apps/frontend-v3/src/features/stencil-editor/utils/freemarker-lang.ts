/**
 * FreeMarker (FTL) 言語サポート for CodeMirror
 * FreeMarker構文のシンタックスハイライトを提供
 */
import { StreamLanguage, LanguageSupport } from '@codemirror/language';
import type { StringStream } from '@codemirror/language';

interface FTLState {
  inDirective: boolean;
  inInterpolation: boolean;
  inComment: boolean;
  inString: boolean;
  stringDelimiter: string | null;
  depth: number;
}

const freemarkerMode = {
  name: 'freemarker',
  
  startState(): FTLState {
    return {
      inDirective: false,
      inInterpolation: false,
      inComment: false,
      inString: false,
      stringDelimiter: null,
      depth: 0,
    };
  },

  token(stream: StringStream, state: FTLState): string | null {
    // コメント処理
    if (state.inComment) {
      if (stream.match('-->', true)) {
        state.inComment = false;
        return 'comment';
      }
      stream.next();
      return 'comment';
    }

    if (stream.match('<!--', true)) {
      state.inComment = true;
      return 'comment';
    }

    // FTLコメント <#-- ... -->
    if (stream.match('<#--', true)) {
      while (stream.current().indexOf('-->') === -1 && !stream.eol()) {
        stream.next();
      }
      if (stream.match('-->', true)) {
        return 'comment';
      }
      return 'comment';
    }

    // 文字列処理
    if (state.inString) {
      if (stream.match(state.stringDelimiter!, true)) {
        state.inString = false;
        state.stringDelimiter = null;
        return 'string';
      }
      if (stream.match('\\\\', true) || stream.match('\\\\"', true) || stream.match("\\\\'", true)) {
        return 'string';
      }
      stream.next();
      return 'string';
    }

    // ディレクティブ内の処理
    if (state.inDirective || state.inInterpolation) {
      // 文字列開始
      if (stream.match('"', true)) {
        state.inString = true;
        state.stringDelimiter = '"';
        return 'string';
      }
      if (stream.match("'", true)) {
        state.inString = true;
        state.stringDelimiter = "'";
        return 'string';
      }

      // ディレクティブ終了
      if (state.inDirective && stream.match('>', true)) {
        state.inDirective = false;
        state.depth = 0;
        return 'punctuation';
      }

      // 補間終了
      if (state.inInterpolation && stream.match('}', true)) {
        state.depth--;
        if (state.depth <= 0) {
          state.inInterpolation = false;
          state.depth = 0;
        }
        return 'punctuation';
      }

      // ネストした { }
      if (stream.match('{', true)) {
        state.depth++;
        return 'punctuation';
      }

      // FreeMarkerキーワード
      if (stream.match(/^(if|else|elseif|list|assign|macro|function|return|break|continue|switch|case|default|include|import|nested|local|global|setting|attempt|recover|fallback|visit|recurse|escape|noescape|compress|noparse|t|lt|rt|nt)\b/, true)) {
        return 'keyword';
      }

      // 組み込み関数
      if (stream.match(/^\?[a-zA-Z_][a-zA-Z0-9_]*/, true)) {
        return 'builtin';
      }

      // 演算子
      if (stream.match(/^[+\-*\/%=!<>&|]+/, true)) {
        return 'operator';
      }

      // 数値
      if (stream.match(/^[0-9]+(\.[0-9]+)?/, true)) {
        return 'number';
      }

      // 変数名
      if (stream.match(/^[a-zA-Z_][a-zA-Z0-9_\.]*/, true)) {
        return 'variableName';
      }

      stream.next();
      return null;
    }

    // ディレクティブ開始 <#...> or </#...> or <@...> or </@...>
    if (stream.match(/<[#@]/, true) || stream.match(/<\/[#@]/, true)) {
      state.inDirective = true;
      state.depth = 0;
      return 'punctuation';
    }

    // 補間開始 ${...}
    if (stream.match('${', true)) {
      state.inInterpolation = true;
      state.depth = 1;
      return 'punctuation';
    }

    // 補間開始 #{...}
    if (stream.match('#{', true)) {
      state.inInterpolation = true;
      state.depth = 1;
      return 'punctuation';
    }

    // HTMLタグ
    if (stream.match(/<\/?[a-zA-Z][a-zA-Z0-9]*/, true)) {
      return 'tagName';
    }

    // HTML属性
    if (stream.match(/^[a-zA-Z-]+(?==)/, true)) {
      return 'attributeName';
    }

    // デフォルト
    stream.next();
    return null;
  },

  languageData: {
    commentTokens: { block: { open: '<#--', close: '-->' } },
  },
};

const freemarkerLanguage = StreamLanguage.define(freemarkerMode);

export function freemarker() {
  return new LanguageSupport(freemarkerLanguage);
}
