/**
 * テンプレートプレビューパネル
 */
import React, { useState, useMemo } from 'react';
import { Button, toast } from '@mirel/ui';
import * as jsYaml from 'js-yaml';
import type { StencilConfig } from '../types';

interface PreviewPanelProps {
  config: StencilConfig;
  yamlContent: string;
  templateContents: Record<string, string>;
  onClose: () => void;
}

export const PreviewPanel: React.FC<PreviewPanelProps> = ({
  yamlContent,
  templateContents,
  onClose,
}) => {
  const [sampleData, setSampleData] = useState<Record<string, string>>({});
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');
  const [previewResult, setPreviewResult] = useState<string>('');

  // YAMLからパラメータ定義を抽出
  const extractParameters = useMemo(() => {
    return (): Array<{ id: string; name: string; type: string }> => {
      try {
        // js-yamlで正確にパース
        const parsed = jsYaml.load(yamlContent) as Record<string, unknown>;
        const stencil = parsed?.stencil as { dataDomain?: Array<{ id?: string; name?: string; type?: string }> } | undefined;
        
        if (!stencil?.dataDomain) {
          console.warn('dataDomainが見つかりません');
          return [];
        }

        const dataDomain = stencil.dataDomain;
        
        if (!Array.isArray(dataDomain)) {
          console.warn('dataDomainが配列ではありません:', dataDomain);
          return [];
        }

        const params = dataDomain
          .filter((item) => item && item.id)
          .map((item) => ({
            id: item.id!,
            name: item.name || item.id!,
            type: item.type || 'text'
          }));

        console.log('抽出されたパラメータ:', params);
        return params;
      } catch (error) {
        console.error('パラメータ抽出エラー:', error);
        return [];
      }
    };
  }, [yamlContent]);

  const parameters = extractParameters();

  // プレビュー生成（簡易版 - FreeMarker変数を置換）
  const generatePreview = () => {
    if (!selectedTemplate) {
      toast({
        title: '選択エラー',
        description: 'テンプレートを選択してください',
        variant: 'warning',
      });
      return;
    }

    let result = templateContents[selectedTemplate] || '';

    // FreeMarker変数を置換（簡易版）
    for (const [key, value] of Object.entries(sampleData)) {
      // ${変数名} を置換
      result = result.replace(new RegExp(`\\$\\{${key}\\}`, 'g'), value);
      // #{変数名} を置換
      result = result.replace(new RegExp(`#\\{${key}\\}`, 'g'), value);
    }

    setPreviewResult(result);
  };

  const templatePaths = Object.keys(templateContents);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-surface rounded-lg shadow-xl max-w-6xl w-full h-5/6 flex flex-col">
        <div className="flex justify-between items-center p-4 border-b border-border">
          <h2 className="text-xl font-bold">テンプレートプレビュー</h2>
          <Button variant="outline" onClick={onClose}>
            閉じる
          </Button>
        </div>

        <div className="flex-1 flex overflow-hidden">
          {/* 左側: サンプルデータ入力 */}
          <div className="w-1/3 border-r border-border p-4 overflow-y-auto">
            <h3 className="font-semibold mb-3">サンプルデータ</h3>

            {/* テンプレート選択 */}
            <div className="mb-4">
              <label className="block text-sm font-medium mb-1">
                テンプレート
              </label>
              <select
                className="w-full border rounded px-2 py-1"
                value={selectedTemplate}
                onChange={(e) => setSelectedTemplate(e.target.value)}
              >
                <option value="">選択してください</option>
                {templatePaths.map((path) => (
                  <option key={path} value={path}>
                    {path}
                  </option>
                ))}
              </select>
            </div>

            {/* パラメータ入力フォーム */}
            {parameters.length > 0 ? (
              <div className="space-y-3">
                {parameters.map((param) => (
                  <div key={param.id}>
                    <label className="block text-sm font-medium mb-1">
                      {param.name || param.id}
                    </label>
                    <input
                      type="text"
                      className="w-full border rounded px-2 py-1 text-sm"
                      value={sampleData[param.id] || ''}
                      onChange={(e) =>
                        setSampleData((prev) => ({
                          ...prev,
                          [param.id]: e.target.value,
                        }))
                      }
                      placeholder={`${param.id}を入力`}
                    />
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                パラメータが定義されていません
              </p>
            )}

            <Button
              className="w-full mt-4"
              onClick={generatePreview}
              disabled={!selectedTemplate}
            >
              プレビュー生成
            </Button>
          </div>

          {/* 右側: プレビュー結果 */}
          <div className="flex-1 p-4 overflow-y-auto">
            <h3 className="font-semibold mb-3">プレビュー結果</h3>
            {previewResult ? (
              <pre className="bg-surface-subtle p-4 rounded border border-border text-sm font-mono overflow-x-auto">
                {previewResult}
              </pre>
            ) : (
              <div className="text-muted-foreground text-center py-8">
                左側でサンプルデータを入力し、「プレビュー生成」ボタンを押してください
              </div>
            )}
          </div>
        </div>

        <div className="p-4 border-t border-border bg-surface-subtle">
          <p className="text-sm text-muted-foreground">
            <strong>注意:</strong> これは簡易プレビューです。実際の生成結果とは異なる場合があります。
            FreeMarkerの制御構文（#if、#list等）は反映されません。
          </p>
        </div>
      </div>
    </div>
  );
};
