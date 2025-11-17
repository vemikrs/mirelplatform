/**
 * メインのステンシルエディタコンポーネント
 */
import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { YamlEditor, YamlEditorHandle } from './YamlEditor';
import { TemplateEditor, TemplateEditorHandle } from './TemplateEditor';
import { ErrorPanel, ValidationError } from './ErrorPanel';
import { VersionHistory } from './VersionHistory';
import { PreviewPanel } from './PreviewPanel';
import { loadStencil, saveStencil } from '../api/stencil-editor-api';
import type { LoadStencilResponse, EditorMode } from '../types';
import { Button } from '@mirel/ui';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@mirel/ui';

export const StencilEditor: React.FC = () => {
  const { stencilId, serial } = useParams<{ stencilId: string; serial: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  
  // クエリパラメータからmodeを取得（デフォルトはview）
  const initialMode = (searchParams.get('mode') as EditorMode) || 'view';
  const [mode, setMode] = useState<EditorMode>(initialMode);
  const [data, setData] = useState<LoadStencilResponse | null>(null);
  const [yamlContent, setYamlContent] = useState('');
  const [templateContents, setTemplateContents] = useState<Record<string, string>>({});
  const [activeTab, setActiveTab] = useState('yaml');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);
  const [autoSaveEnabled, setAutoSaveEnabled] = useState(true);
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  
  const autoSaveTimerRef = useRef<NodeJS.Timeout | null>(null);
  
  const yamlEditorRef = useRef<YamlEditorHandle>(null);
  const templateEditorRefs = useRef<Record<string, TemplateEditorHandle>>({});

  // データ読込
  useEffect(() => {
    if (stencilId && serial) {
      loadData(stencilId, serial);
    }
  }, [stencilId, serial]);

  // 自動保存タイマー
  useEffect(() => {
    if (!autoSaveEnabled || mode !== 'edit' || !hasUnsavedChanges) {
      return;
    }

    // エラーがある場合は自動保存しない
    const errors = validationErrors.filter(e => e.severity === 'error');
    if (errors.length > 0) {
      return;
    }

    // 30秒後に自動保存
    if (autoSaveTimerRef.current) {
      clearTimeout(autoSaveTimerRef.current);
    }

    autoSaveTimerRef.current = setTimeout(() => {
      handleSave(true);
    }, 30000);

    return () => {
      if (autoSaveTimerRef.current) {
        clearTimeout(autoSaveTimerRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [yamlContent, templateContents, autoSaveEnabled, mode, hasUnsavedChanges, validationErrors]);

  // 内容変更検知
  useEffect(() => {
    if (mode === 'edit' && data) {
      const originalYaml = data.files.find(f => f.name === 'stencil-settings.yml')?.content || '';
      const hasYamlChange = yamlContent !== originalYaml;
      
      const hasTemplateChange = data.files
        .filter(f => f.type === 'template')
        .some(f => templateContents[f.path] !== f.content);
      
      setHasUnsavedChanges(hasYamlChange || hasTemplateChange);
    } else {
      setHasUnsavedChanges(false);
    }
  }, [yamlContent, templateContents, mode, data]);

  // キーボードショートカット
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ctrl+S / Cmd+S: 保存
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        if (mode === 'edit' && !saving) {
          handleSave(false);
        }
      }

      // Ctrl+E / Cmd+E: 編集モード切替
      if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
        e.preventDefault();
        setMode(mode === 'view' ? 'edit' : 'view');
      }

      // Escape: 一覧へ戻る（確認あり）
      if (e.key === 'Escape') {
        if (hasUnsavedChanges) {
          const confirm = window.confirm('未保存の変更があります。一覧へ戻りますか?');
          if (confirm) {
            navigate('/promarker/stencils');
          }
        } else {
          navigate('/promarker/stencils');
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode, saving, hasUnsavedChanges, navigate]);

  const loadData = async (id: string, ser: string) => {
    setLoading(true);
    try {
      const result = await loadStencil(id, ser);
      setData(result);
      
      // テンプレートファイルを抽出
      const templates: Record<string, string> = {};
      result.files
        .filter(f => f.type === 'template')
        .forEach(f => {
          templates[f.path] = f.content;
        });
      setTemplateContents(templates);
      
      // stencil-settings.ymlを抽出
      const settingsFile = result.files.find(f => f.name === 'stencil-settings.yml');
      if (settingsFile) {
        setYamlContent(settingsFile.content);
      }
    } catch (error) {
      console.error('読込エラー:', error);
      alert('ステンシルの読み込みに失敗しました');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (isAutoSave = false) => {
    if (!data || !stencilId || !serial) return;

    // バリデーションエラーがある場合は警告
    const errors = validationErrors.filter(e => e.severity === 'error');
    if (errors.length > 0) {
      if (!isAutoSave) {
        const confirm = window.confirm(
          `${errors.length}件のエラーがあります。このまま保存しますか？`
        );
        if (!confirm) return;
      } else {
        // 自動保存の場合はエラー時スキップ
        return;
      }
    }

    setSaving(true);
    try {
      // ファイルリストを更新
      const updatedFiles = data.files.map(f => {
        if (f.name === 'stencil-settings.yml') {
          return { ...f, content: yamlContent };
        }
        if (f.type === 'template' && templateContents[f.path]) {
          return { ...f, content: templateContents[f.path] };
        }
        return f;
      });

      await saveStencil({
        stencilId,
        serial,
        config: data.config,
        files: updatedFiles,
        message: isAutoSave ? '自動保存' : '編集保存',
      });

      // 保存成功時の処理
      setLastSaved(new Date());
      setHasUnsavedChanges(false);
      
      // データを更新（最新の内容でリフレッシュ）
      setData({
        ...data,
        files: updatedFiles
      });

      if (!isAutoSave) {
        alert('保存しました');
        // 一覧へ戻る
        navigate('/promarker/stencils');
      }
    } catch (error) {
      console.error('保存エラー:', error);
      if (!isAutoSave) {
        alert('保存に失敗しました');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleErrorClick = (error: ValidationError) => {
    // エラー位置にスクロール
    if (error.file === 'stencil-settings.yml' && error.line && yamlEditorRef.current) {
      setActiveTab('yaml');
      setTimeout(() => yamlEditorRef.current?.scrollToLine(error.line!), 100);
    } else if (error.file && templateEditorRefs.current[error.file] && error.line) {
      setActiveTab('templates');
      setTimeout(() => templateEditorRefs.current[error.file]?.scrollToLine(error.line!), 100);
    }
  };

  // バージョン復元ハンドラー
  const handleRestore = async (serial: string) => {
    if (!stencilId) return;

    try {
      setLoading(true);
      
      // 指定バージョンのデータを読込
      const restoredData = await loadStencil(stencilId, serial);
      
      // 現在のデータとして設定
      setData(restoredData);
      
      // YAML内容を更新
      const settingsFile = restoredData.files.find(f => f.name === 'stencil-settings.yml');
      if (settingsFile) {
        setYamlContent(settingsFile.content);
      }
      
      // テンプレート内容を更新
      const templates: Record<string, string> = {};
      restoredData.files
        .filter(f => f.type === 'template')
        .forEach(f => {
          templates[f.path] = f.content;
        });
      setTemplateContents(templates);
      
      // 編集モードに切り替え
      setMode('edit');
      setActiveTab('yaml');
      
      alert(`バージョン ${serial} を復元しました。編集後に保存してください。`);
    } catch (error) {
      console.error('復元エラー:', error);
      alert('バージョンの復元に失敗しました');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="p-4">読み込み中...</div>;
  }

  if (!data) {
    return <div className="p-4">データがありません</div>;
  }

  return (
    <div className="stencil-editor p-4">
      <div className="flex justify-between items-center mb-4">
        <div>
          <h1 className="text-2xl font-bold">{data.config.name}</h1>
          {mode === 'edit' && (
            <div className="text-sm text-gray-600 mt-1">
              {saving && '保存中...'}
              {!saving && hasUnsavedChanges && '未保存の変更があります'}
              {!saving && !hasUnsavedChanges && lastSaved && `最終保存: ${lastSaved.toLocaleTimeString()}`}
              {!saving && !hasUnsavedChanges && !lastSaved && '変更なし'}
            </div>
          )}
        </div>
        <div className="flex gap-2 items-center">
          {mode === 'edit' && (
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={autoSaveEnabled}
                onChange={(e) => setAutoSaveEnabled(e.target.checked)}
                className="rounded"
              />
              自動保存(30秒)
            </label>
          )}
          <Button
            variant="outline"
            onClick={() => setMode(mode === 'view' ? 'edit' : 'view')}
          >
            {mode === 'view' ? '編集モード' : '参照モード'}
          </Button>
          {mode === 'edit' && (
            <Button onClick={() => handleSave(false)} disabled={saving}>
              {saving ? '保存中...' : '保存'}
            </Button>
          )}
          <Button variant="outline" onClick={() => setShowPreview(true)}>
            プレビュー
          </Button>
          <Button variant="outline" onClick={() => navigate('/promarker/stencils')}>
            一覧へ戻る
          </Button>
        </div>
      </div>

      {/* キーボードショートカットのヘルプ */}
      <div className="mb-4 p-2 bg-blue-50 rounded text-sm text-gray-700">
        <strong>ショートカット:</strong>
        {' '}
        <kbd className="px-1 py-0.5 bg-white border rounded">Ctrl+S</kbd> 保存
        {' '}|{' '}
        <kbd className="px-1 py-0.5 bg-white border rounded">Ctrl+E</kbd> モード切替
        {' '}|{' '}
        <kbd className="px-1 py-0.5 bg-white border rounded">Esc</kbd> 一覧へ戻る
      </div>

      {/* エラーパネル */}
      <ErrorPanel errors={validationErrors} onErrorClick={handleErrorClick} />

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="yaml">YAML設定</TabsTrigger>
          <TabsTrigger value="templates">テンプレート</TabsTrigger>
          <TabsTrigger value="files">その他ファイル</TabsTrigger>
          <TabsTrigger value="history">履歴</TabsTrigger>
        </TabsList>

        <TabsContent value="yaml" className="mt-4">
          <YamlEditor
            ref={yamlEditorRef}
            value={yamlContent}
            onChange={setYamlContent}
            onValidationChange={(errors) => {
              // YAMLエラーのみ更新
              setValidationErrors(prev => [
                ...prev.filter(e => e.file !== 'stencil-settings.yml'),
                ...errors
              ]);
            }}
            readOnly={mode === 'view'}
          />
        </TabsContent>

        <TabsContent value="templates" className="mt-4">
          <div className="space-y-4">
            {data.files
              .filter(f => f.type === 'template')
              .map(f => (
                <div key={f.path} className="border rounded overflow-hidden">
                  <div className="bg-gray-100 px-4 py-2 font-mono text-sm font-semibold">
                    {f.path}
                  </div>
                  <TemplateEditor
                    ref={el => {
                      if (el) templateEditorRefs.current[f.path] = el;
                    }}
                    value={templateContents[f.path] || f.content}
                    onChange={(content) => {
                      setTemplateContents(prev => ({
                        ...prev,
                        [f.path]: content
                      }));
                    }}
                    fileName={f.name}
                    onValidationChange={(errors) => {
                      // 該当ファイルのエラーのみ更新
                      setValidationErrors(prev => [
                        ...prev.filter(e => e.file !== f.path),
                        ...errors
                      ]);
                    }}
                    readOnly={mode === 'view'}
                  />
                </div>
              ))}
          </div>
        </TabsContent>

        <TabsContent value="files" className="mt-4">
          <div className="space-y-2">
            {data.files
              .filter(f => f.type === 'other')
              .map(f => (
                <div key={f.path} className="p-2 border rounded">
                  <div className="font-mono text-sm">{f.path}</div>
                </div>
              ))}
          </div>
        </TabsContent>

        <TabsContent value="history" className="mt-4">
          {stencilId && serial && (
            <VersionHistory
              stencilId={stencilId}
              currentSerial={serial}
              onRestore={handleRestore}
            />
          )}
        </TabsContent>
      </Tabs>

      {/* プレビューパネル */}
      {showPreview && (
        <PreviewPanel
          config={data.config}
          yamlContent={yamlContent}
          templateContents={templateContents}
          onClose={() => setShowPreview(false)}
        />
      )}
    </div>
  );
};
