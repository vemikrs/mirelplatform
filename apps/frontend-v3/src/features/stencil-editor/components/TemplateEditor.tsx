/**
 * FreeMarker Template (FTL) エディタコンポーネント
 */
import React, { useRef, useImperativeHandle } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { linter, Diagnostic } from '@codemirror/lint';
import { EditorView } from '@codemirror/view';
import { ftlValidator } from '../utils/ftl-validator';
import type { ValidationError } from './ErrorPanel';

interface TemplateEditorProps {
  value: string;
  onChange: (value: string) => void;
  fileName: string;
  onValidationChange?: (errors: ValidationError[]) => void;
  readOnly?: boolean;
  expectedVariables?: string[];
}

export interface TemplateEditorHandle {
  scrollToLine: (line: number) => void;
}

export const TemplateEditor = React.forwardRef<
  TemplateEditorHandle,
  TemplateEditorProps
>(
  (
    {
      value,
      onChange,
      fileName,
      onValidationChange,
      readOnly = false,
      expectedVariables = [],
    },
    ref
  ) => {
    const editorRef = useRef<EditorView | null>(null);

    // FTL構文バリデーション用のlinter
    const ftlLinter = linter((view) => {
      const diagnostics: Diagnostic[] = [];
      const text = view.state.doc.toString();

      // FTL構文チェック
      const errors = ftlValidator.validate(text, fileName);

      // 期待される変数のチェック（オプション）
      if (expectedVariables.length > 0) {
        const varErrors = ftlValidator.checkCommonVariables(
          text,
          expectedVariables
        );
        errors.push(...varErrors);
      }

      // エラーをCodeMirror診断情報に変換
      errors.forEach((error) => {
        const lineNumber = error.line || 1;
        const line = view.state.doc.line(Math.min(lineNumber, view.state.doc.lines));
        
        diagnostics.push({
          from: line.from,
          to: line.to,
          severity: error.severity,
          message: error.message,
        });
      });

      // エラー情報を親コンポーネントに通知
      onValidationChange?.(errors);

      return diagnostics;
    });

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
      <div className="template-editor">
        <CodeMirror
          value={value}
          height="600px"
          extensions={[ftlLinter]}
          onChange={onChange}
          onCreateEditor={(view) => {
            editorRef.current = view;
          }}
          editable={!readOnly}
          basicSetup={{
            lineNumbers: true,
            highlightActiveLineGutter: true,
            highlightSpecialChars: true,
            foldGutter: true,
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

TemplateEditor.displayName = 'TemplateEditor';
