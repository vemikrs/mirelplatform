/**
 * CodeMirrorベースのYAMLエディタコンポーネント
 */
import React from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { yaml } from '@codemirror/lang-yaml';
import { linter, Diagnostic } from '@codemirror/lint';
import { StencilConfigSchema } from '../schemas';
import { z } from 'zod';
import * as jsYaml from 'js-yaml';

interface YamlEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
}

export const YamlEditor: React.FC<YamlEditorProps> = ({
  value,
  onChange,
  readOnly = false,
}) => {
  // YAMLバリデーション用のlinter
  const yamlLinter = linter((view) => {
    const diagnostics: Diagnostic[] = [];
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
            diagnostics.push({
              from: 0,
              to: text.length,
              severity: 'error',
              message: `${error.path.join('.')}: ${error.message}`,
            });
          });
        }
      }
    } catch (error) {
      // YAML構文エラー
      diagnostics.push({
        from: 0,
        to: text.length,
        severity: 'error',
        message: error instanceof Error ? error.message : 'YAML構文エラー',
      });
    }

    return diagnostics;
  });

  return (
    <div className="yaml-editor">
      <CodeMirror
        value={value}
        height="600px"
        extensions={[yaml(), yamlLinter]}
        onChange={onChange}
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
};
