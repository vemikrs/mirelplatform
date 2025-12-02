import type { ValidationError } from '../components/ErrorPanel';

/**
 * FreeMarker Template Language (FTL) の基本的な構文チェック
 */
export class FtlValidator {
  /**
   * FTLテンプレートの構文を検証
   */
  validate(content: string, fileName: string): ValidationError[] {
    const errors: ValidationError[] = [];
    const lines = content.split('\n');

    // 1. ディレクティブのバランスチェック
    errors.push(...this.checkDirectiveBalance(lines, fileName));

    // 2. 変数参照の構文チェック
    errors.push(...this.checkVariableSyntax(lines, fileName));

    // 3. コメントの構文チェック
    errors.push(...this.checkCommentSyntax(lines, fileName));

    // 4. 補間の構文チェック
    errors.push(...this.checkInterpolationSyntax(lines, fileName));

    return errors;
  }

  /**
   * ディレクティブの開始/終了タグのバランスをチェック
   */
  private checkDirectiveBalance(
    lines: string[],
    fileName: string
  ): ValidationError[] {
    const errors: ValidationError[] = [];
    const stack: Array<{ directive: string; line: number }> = [];

    // 開始/終了タグのパターン
    const openPattern = /<#(if|list|macro|function|switch|assign|global|local|setting|compress|escape|noescape|attempt)[\s>]/g;
    const closePattern = /<\/#(if|list|macro|function|switch|assign|global|local|setting|compress|escape|noescape|attempt)>/g;

    lines.forEach((line, lineIndex) => {
      const lineNumber = lineIndex + 1;

      // 開始タグを検出
      let match;
      while ((match = openPattern.exec(line)) !== null) {
        const directive = match[1];
        if (directive) {
          stack.push({ directive, line: lineNumber });
        }
      }

      // 終了タグを検出
      while ((match = closePattern.exec(line)) !== null) {
        const directive = match[1];
        if (!directive) continue;
        
        const last = stack.pop();

        if (!last) {
          errors.push({
            severity: 'error',
            message: `対応する開始タグがありません: </#${directive}>`,
            line: lineNumber,
            file: fileName,
          });
        } else if (last.directive !== directive) {
          errors.push({
            severity: 'error',
            message: `ディレクティブの不一致: <#${last.directive}> と </#${directive}>`,
            line: lineNumber,
            file: fileName,
          });
        }
      }
    });

    // 閉じられていないタグをエラーとして報告
    stack.forEach((item) => {
      errors.push({
        severity: 'error',
        message: `閉じられていないディレクティブ: <#${item.directive}>`,
        line: item.line,
        file: fileName,
      });
    });

    return errors;
  }

  /**
   * 変数参照の構文をチェック
   */
  private checkVariableSyntax(
    lines: string[],
    fileName: string
  ): ValidationError[] {
    const errors: ValidationError[] = [];

    // ${variable} または ${variable?default} パターン
    const variablePattern = /\$\{([^}]*)\}/g;

    lines.forEach((line, lineIndex) => {
      const lineNumber = lineIndex + 1;
      let match;

      while ((match = variablePattern.exec(line)) !== null) {
        const expression = match[1];
        if (!expression) continue;

        // 空の変数参照
        if (!expression.trim()) {
          errors.push({
            severity: 'error',
            message: '変数名が空です: ${}',
            line: lineNumber,
            column: match.index + 1,
            file: fileName,
          });
          continue;
        }

        // 不正な文字をチェック
        if (!/^[a-zA-Z_][\w.?!]*(\([^)]*\))?(\[[^\]]*\])*$/.test(expression)) {
          errors.push({
            severity: 'warning',
            message: `不正な変数参照の可能性: \${${expression}}`,
            line: lineNumber,
            column: match.index + 1,
            file: fileName,
          });
        }
      }
    });

    return errors;
  }

  /**
   * コメントの構文をチェック
   */
  private checkCommentSyntax(
    lines: string[],
    fileName: string
  ): ValidationError[] {
    const errors: ValidationError[] = [];
    let inComment = false;
    let commentStartLine = 0;

    lines.forEach((line, lineIndex) => {
      const lineNumber = lineIndex + 1;

      if (line.includes('<#--')) {
        if (inComment) {
          errors.push({
            severity: 'warning',
            message: 'ネストされたコメント開始タグ',
            line: lineNumber,
            file: fileName,
          });
        }
        inComment = true;
        commentStartLine = lineNumber;
      }

      if (line.includes('-->')) {
        if (!inComment) {
          errors.push({
            severity: 'error',
            message: '対応するコメント開始タグがありません',
            line: lineNumber,
            file: fileName,
          });
        }
        inComment = false;
      }
    });

    // 閉じられていないコメント
    if (inComment) {
      errors.push({
        severity: 'error',
        message: '閉じられていないコメント',
        line: commentStartLine,
        file: fileName,
      });
    }

    return errors;
  }

  /**
   * 補間構文をチェック
   */
  private checkInterpolationSyntax(
    lines: string[],
    fileName: string
  ): ValidationError[] {
    const errors: ValidationError[] = [];
    const fullContent = lines.join('\n');

    // コメント部分を除外
    const withoutComments = fullContent.replace(/<#--[\s\S]*?-->/g, '');

    // 文字列リテラル内を除外（簡易版）
    const withoutStrings = withoutComments.replace(/'[^']*'/g, '').replace(/"[^"]*"/g, '');

    // ${ と } のバランスをチェック
    let balance = 0;
    let interpolationStart = -1;
    
    for (let i = 0; i < withoutStrings.length; i++) {
      if (i < withoutStrings.length - 1 && withoutStrings[i] === '$' && withoutStrings[i + 1] === '{') {
        if (balance === 0) {
          interpolationStart = i;
        }
        balance++;
        i++; // Skip '{'
      } else if (withoutStrings[i] === '}' && balance > 0) {
        balance--;
      }
    }

    // 閉じられていない ${ がある場合のみエラー
    if (balance > 0) {
      // エラーが発生している行を特定
      let charCount = 0;
      for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
        const line = lines[lineIndex];
        if (line === undefined) continue;
        charCount += line.length + 1; // +1 for newline
        if (charCount > interpolationStart) {
          errors.push({
            severity: 'error',
            message: '閉じられていない補間: ${',
            line: lineIndex + 1,
            file: fileName,
          });
          break;
        }
      }
    }

    return errors;
  }

  /**
   * よく使われる変数の存在チェック（オプション）
   */
  checkCommonVariables(
    content: string,
    expectedVars: string[]
  ): ValidationError[] {
    const errors: ValidationError[] = [];
    const variablePattern = /\$\{([a-zA-Z_][\w.]*)/g;
    const usedVars = new Set<string>();

    let match;
    while ((match = variablePattern.exec(content)) !== null) {
      if (match[1]) {
        const parts = match[1].split('.');
        const varName = parts[0] || '';
        if (varName) {
          usedVars.add(varName);
        }
      }
    }

    expectedVars.forEach((varName) => {
      if (!usedVars.has(varName)) {
        errors.push({
          severity: 'info',
          message: `期待される変数が使用されていません: ${varName}`,
          file: 'template',
        });
      }
    });

    return errors;
  }
}

// シングルトンインスタンス
export const ftlValidator = new FtlValidator();
