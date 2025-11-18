/**
 * CodeMirrorベースのYAMLエディタコンポーネント
 */
import React, { useRef, useImperativeHandle } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { yaml } from '@codemirror/lang-yaml';
import { linter } from '@codemirror/lint';
import type { Diagnostic } from '@codemirror/lint';
import { EditorView } from '@codemirror/view';
import { StencilConfigSchema } from '../schemas';
import * as jsYaml from 'js-yaml';
import type { ValidationError } from './ErrorPanel';

interface YamlEditorProps {
  value: string;
  onChange: (value: string) => void;
  onValidationChange?: (errors: ValidationError[]) => void;
  readOnly?: boolean;
}

export interface YamlEditorHandle {
  scrollToLine: (line: number) => void;
}

export const YamlEditor = React.forwardRef<YamlEditorHandle, YamlEditorProps>(
  ({ value, onChange, onValidationChange, readOnly = false }, ref) => {
    const editorRef = useRef<EditorView | null>(null);
    const previousErrorsRef = useRef<string>('');
    
  // YAMLバリデーション用のlinter
  const yamlLinter = linter((view) => {
    const diagnostics: Diagnostic[] = [];
    const validationErrors: ValidationError[] = [];
    const text = view.state.doc.toString();

    try {
      // YAML構文チェック
      const parsed = jsYaml.load(text) as unknown;

      // Zodスキーマバリデーション
      if (parsed && typeof parsed === 'object') {
        const parsedObj = parsed as any;
        
        if (!parsedObj.stencil) {
          validationErrors.push({
            severity: 'error',
            message: 'stencil: 必須フィールドがありません',
            line: 1,
            file: 'stencil-settings.yml',
          });
          diagnostics.push({
            from: 0,
            to: 10,
            severity: 'error',
            message: 'stencil: 必須フィールドがありません',
          });
        } else if (!parsedObj.stencil.config) {
          validationErrors.push({
            severity: 'error',
            message: 'stencil.config: 必須フィールドがありません',
            line: 2,
            file: 'stencil-settings.yml',
          });
          diagnostics.push({
            from: 0,
            to: 20,
            severity: 'error',
            message: 'stencil.config: 必須フィールドがありません',
          });
        } else {
          const result = StencilConfigSchema.safeParse(parsedObj.stencil.config);
          
          if (!result.success) {
            // Zodエラーを診断情報に変換
            result.error.errors.forEach((error) => {
              const errorPath = error.path.length > 0 ? error.path.join('.') : 'config';
              const fieldName = error.path[error.path.length - 1] || 'config';
              const message = `${errorPath}: ${error.message} (期待される型: ${error.code})`;
              
              // CodeMirror診断情報
              const lineNumber = findLineForPath(text, fieldName.toString());
              const from = lineNumber > 0 ? view.state.doc.line(lineNumber).from : 0;
              const to = lineNumber > 0 ? view.state.doc.line(lineNumber).to : text.length;
              
              diagnostics.push({
                from,
                to,
                severity: 'error',
                message,
              });

              // ValidationError形式
              validationErrors.push({
                severity: 'error',
                message,
                line: lineNumber,
                file: 'stencil-settings.yml',
              });
            });
          }
        }
      }
    } catch (error) {
      // YAML構文エラー
      const errorMessage = error instanceof Error ? error.message : 'YAML構文エラー';
      const lineNumber = extractLineNumber(errorMessage);
      
      diagnostics.push({
        from: 0,
        to: text.length,
        severity: 'error',
        message: errorMessage,
      });

      validationErrors.push({
        severity: 'error',
        message: errorMessage,
        line: lineNumber,
        file: 'stencil-settings.yml',
      });
    }

    // エラー情報を親コンポーネントに通知（変更があった場合のみ）
    const currentErrorsKey = JSON.stringify(validationErrors.map(e => `${e.message}:${e.line}`));
    if (currentErrorsKey !== previousErrorsRef.current) {
      previousErrorsRef.current = currentErrorsKey;
      onValidationChange?.(validationErrors);
    }

    return diagnostics;
  });

  // YAMLテキストから特定のパスの行番号を探す
  const findLineForPath = (text: string, path: string): number => {
    const lines = text.split('\n');
    const pathParts = path.split('.');
    const searchKey = pathParts[pathParts.length - 1];
    
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].includes(searchKey + ':')) {
        return i + 1;
      }
    }
    return 0;
  };

  // エラーメッセージから行番号を抽出
  const extractLineNumber = (message: string): number | undefined => {
    const match = message.match(/line (\d+)/);
    return match ? parseInt(match[1], 10) : undefined;
  };

  // 特定の行にスクロール
  useImperativeHandle(ref, () => ({
    scrollToLine: (line: number) => {
      if (editorRef.current) {
        const pos = editorRef.current.state.doc.line(line).from;
        editorRef.current.dispatch({
          selection: { anchor: pos },
          scrollIntoView: true,
        });
      }
    },
  }));

  return (
    <div className="yaml-editor">
      <CodeMirror
        value={value}
        height="600px"
        extensions={[yaml(), yamlLinter]}
        onChange={onChange}
        onCreateEditor={(view) => {
          editorRef.current = view;
        }}
        editable={!readOnly}
        basicSetup={{
          lineNumbers: true,
          highlightActiveLineGutter: true,
          highlightSpecialChars: true,
          foldGursor: true,
          drawSelection: true,
          dropCursor: true,
          allowMultipleSelections: true,
          indentOnInput: true,
          bracketMatching: true,
          closeBrackets: true,
          autocompletion: true,
          rectangularSelection: true,
          crosshairCursor: true,
          highlightActiveLine: true,
          highlightSelectionMatches: true,
          closeBracketsKeymap: true,
          searchKeymap: true,
          foldKeymap: true,
          completionKeymap: true,
          lintKeymap: true,
        }}
        theme="light"
      />
    </div>
  );
  }
);

YamlEditor.displayName = 'YamlEditor';
