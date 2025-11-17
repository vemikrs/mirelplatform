/**
 * CodeMirrorベースのYAMLエディタコンポーネント
 */
import React, { useRef, useImperativeHandle } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { yaml } from '@codemirror/lang-yaml';
import { linter, Diagnostic } from '@codemirror/lint';
import { EditorView } from '@codemirror/view';
import { StencilConfigSchema } from '../schemas';
import { z } from 'zod';
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
  // YAMLバリデーション用のlinter
  const yamlLinter = linter((view) => {
    const diagnostics: Diagnostic[] = [];
    const validationErrors: ValidationError[] = [];
    const text = view.state.doc.toString();

    try {
      // YAML構文チェック
      const parsed = jsYaml.load(text) as any;

      // Zodスキーマバリデーション
      if (parsed && parsed.stencil && parsed.stencil.config) {
        const result = StencilConfigSchema.safeParse(parsed.stencil.config);
        
        if (!result.success) {
          // Zodエラーを診断情報に変換
          result.error.errors.forEach((error) => {
            const errorPath = error.path.join('.');
            const message = `${errorPath}: ${error.message}`;
            
            // CodeMirror診断情報
            const lineNumber = findLineForPath(text, errorPath);
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

    // エラー情報を親コンポーネントに通知
    onValidationChange?.(validationErrors);

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
