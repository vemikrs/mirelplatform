/**
 * メインのステンシルエディタコンポーネント
 */
import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { YamlEditor } from './YamlEditor';
import type { YamlEditorHandle } from './YamlEditor';
import { TemplateEditor } from './TemplateEditor';
import type { TemplateEditorHandle } from './TemplateEditor';
import { ErrorPanel } from './ErrorPanel';
import type { ValidationError } from './ErrorPanel';
import { VersionHistory } from './VersionHistory';
import { PreviewPanel } from './PreviewPanel';
import { FileExplorer } from './FileExplorer';
import { Tabs, TabsContent, TabsList, TabsTrigger, Button, toast, Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@mirel/ui';
import { loadStencil, saveStencil } from '../api/stencil-editor-api';
import type { LoadStencilResponse, EditorMode } from '../types';

export const StencilEditor: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const location = window.location.pathname;
  
  // URLから stencilId と serial を抽出（例: /promarker/editor/springboot/service171/221208A）
  const pathParts = location.split('/').filter(Boolean);
  const editorIndex = pathParts.indexOf('editor');
  
  // editor以降のパスを解析
  // 最後の要素がシリアル番号（例: 221208A）
  const serial = pathParts[pathParts.length - 1];
  // editor の次から最後の1つ前までがstencilId（例: /springboot/service171）
  const stencilId = '/' + pathParts.slice(editorIndex + 1, -1).join('/');
  
  console.log('🔍 StencilEditor URL解析:', {
    location,
    pathParts,
    editorIndex,
    stencilId,
    serial,
  });
  
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
  const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);
  const [explorerWidth, setExplorerWidth] = useState(280);
  const [isResizing, setIsResizing] = useState(false);
  
  const autoSaveTimerRef = useRef<NodeJS.Timeout | null>(null);
  const explorerRef = useRef<HTMLDivElement>(null);
  
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

      const hasTemplateChange = Object.keys(templateContents).some(path => {
        const original = data.files.find(f => f.path === path)?.content || '';
        return templateContents[path] !== original;
      });

      setHasUnsavedChanges(hasYamlChange || hasTemplateChange);
    }
  }, [yamlContent, templateContents, mode, data]);

  // エクスプローラーリサイズ処理
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isResizing) return;
      const newWidth = e.clientX;
      if (newWidth >= 200 && newWidth <= 600) {
        setExplorerWidth(newWidth);
      }
    };

    const handleMouseUp = () => {
      setIsResizing(false);
    };

    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isResizing]);

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
      
      // APIレスポンスの検証
      if (!result) {
        throw new Error('APIレスポンスが空です');
      }
      
      if (!result.files || !Array.isArray(result.files)) {
        console.error('無効なAPIレスポンス:', result);
        throw new Error('ファイル一覧が取得できませんでした');
      }
      
      setData(result);
      
      // テンプレートファイルを抽出
      const templates: Record<string, string> = {};
      result.files
        .filter(f => f && f.type === 'template')
        .forEach(f => {
          templates[f.path] = f.content;
        });
      setTemplateContents(templates);
      
      // stencil-settings.ymlを抽出
      const settingsFile = result.files.find(f => f && f.name === 'stencil-settings.yml');
      if (settingsFile) {
        setYamlContent(settingsFile.content);
      } else {
        throw new Error('stencil-settings.ymlが見つかりませんでした');
      }
    } catch (error) {
      console.error('読込エラー:', error);
      const message = error instanceof Error ? error.message : 'ステンシルの読み込みに失敗しました';
      toast({
        title: '読み込みエラー',
        description: message,
        variant: 'destructive',
      });
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
      const updatedFiles = (data.files || []).map(f => {
        if (f && f.name === 'stencil-settings.yml') {
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
        toast({
          title: '保存完了',
          description: 'ステンシルを保存しました',
          variant: 'success',
        });
        // 一覧へ戻る
        navigate('/promarker/stencils');
      }
    } catch (error) {
      console.error('保存エラー:', error);
      if (!isAutoSave) {
        toast({
          title: '保存エラー',
          description: '保存に失敗しました',
          variant: 'destructive',
        });
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

  // ファイル選択ハンドラー
  const handleFileSelect = (file: typeof data.files[0]) => {
    setSelectedFilePath(file.path);
    
    if (file.name === 'stencil-settings.yml') {
      setActiveTab('yaml');
    } else if (file.type === 'template') {
      setActiveTab('templates');
    } else {
      setActiveTab('files');
    }
  };

  // ファイル名変更ハンドラー
  const handleFileRename = (oldPath: string, newPath: string) => {
    if (!data) return;
    
    setData({
      ...data,
      files: data.files.map(f => 
        f.path === oldPath ? { ...f, path: newPath, name: newPath.split('/').pop() || f.name } : f
      )
    });
    
    setHasUnsavedChanges(true);
    
    toast({
      title: 'ファイル名変更',
      description: `${oldPath} → ${newPath}`,
      variant: 'success',
    });
  };

  // ファイル作成ハンドラー
  const handleFileCreate = (parentPath: string, fileName: string) => {
    if (!data) return;
    
    const newPath = parentPath === '/' ? `/${fileName}` : `${parentPath}/${fileName}`;
    
    // 既存ファイルチェック
    if (data.files.some(f => f.path === newPath)) {
      toast({
        title: '作成エラー',
        description: '同名のファイルが既に存在します',
        variant: 'destructive',
      });
      return;
    }
    
    const newFile = {
      path: newPath,
      name: fileName,
      content: '',
      type: fileName.endsWith('.ftl') ? 'template' as const : 'other' as const,
      isEditable: true,
    };
    
    setData({
      ...data,
      files: [...data.files, newFile]
    });
    
    setHasUnsavedChanges(true);
    
    toast({
      title: 'ファイル作成',
      description: `${newPath} を作成しました`,
      variant: 'success',
    });
  };

  // ファイル削除ハンドラー
  const handleFileDelete = (path: string) => {
    if (!data) return;
    
    setData({
      ...data,
      files: data.files.filter(f => f.path !== path)
    });
    
    setHasUnsavedChanges(true);
    
    toast({
      title: 'ファイル削除',
      description: `${path} を削除しました`,
      variant: 'success',
    });
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
      
      toast({
        title: 'バージョン復元',
        description: `バージョン ${serial} を復元しました。編集後に保存してください。`,
        variant: 'success',
      });
    } catch (error) {
      console.error('バージョン復元エラー:', error);
      toast({
        title: '復元エラー',
        description: 'バージョンの復元に失敗しました',
        variant: 'destructive',
      });
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

      {/* メインレイアウト: エクスプローラー + タブ */}
      <div className="flex gap-0 h-[calc(100vh-280px)]">
        {/* 左側: ファイルエクスプローラー */}
        <div 
          ref={explorerRef}
          className="border-r bg-gray-50 overflow-hidden flex-shrink-0"
          style={{ width: `${explorerWidth}px` }}
        >
          <FileExplorer
            files={data.files}
            currentFilePath={selectedFilePath}
            onFileSelect={handleFileSelect}
            onFileRename={handleFileRename}
            onFileCreate={handleFileCreate}
            onFileDelete={handleFileDelete}
            readOnly={mode === 'view'}
          />
        </div>

        {/* リサイズハンドル */}
        <div
          className="w-1 bg-gray-200 hover:bg-blue-400 cursor-col-resize active:bg-blue-500 transition-colors"
          onMouseDown={() => setIsResizing(true)}
        />

        {/* 右側: タブコンテンツ */}
        <div className="flex-1 overflow-hidden">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="h-full flex flex-col">
            <TabsList>
              <TabsTrigger value="yaml">YAML設定</TabsTrigger>
              <TabsTrigger value="templates">テンプレート</TabsTrigger>
              <TabsTrigger value="files">その他ファイル</TabsTrigger>
              <TabsTrigger value="history">履歴</TabsTrigger>
            </TabsList>

            <div className="flex-1 overflow-auto">
              <TabsContent value="yaml" className="mt-4 h-full">
                <YamlEditor
                  ref={yamlEditorRef}
                  value={yamlContent}
                  onChange={setYamlContent}
                  onValidationChange={(errors) => {
                    setValidationErrors(prev => [
                      ...prev.filter(e => e.file !== 'stencil-settings.yml'),
                      ...errors
                    ]);
                  }}
                  readOnly={mode === 'view'}
                />
              </TabsContent>

              <TabsContent value="templates" className="mt-4">
                <Accordion type="multiple" defaultValue={[]} className="space-y-2">
                  {data.files
                    .filter(f => f.type === 'template')
                    .map((f, index) => (
                      <AccordionItem key={f.path} value={`template-${index}`} className="border rounded overflow-hidden">
                        <AccordionTrigger className="bg-gray-100 px-4 py-2 hover:bg-gray-200 font-mono text-sm font-semibold">
                          {f.path}
                        </AccordionTrigger>
                        <AccordionContent className="p-0">
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
                              setValidationErrors(prev => [
                                ...prev.filter(e => e.file !== f.path),
                                ...errors
                              ]);
                            }}
                            readOnly={mode === 'view'}
                          />
                        </AccordionContent>
                      </AccordionItem>
                    ))}
                </Accordion>
              </TabsContent>

              <TabsContent value="files" className="mt-4">
                <div className="space-y-2">
                  {data.files
                    .filter(f => f.type === 'other')
                    .map(f => (
                      <div key={f.path} className="p-2 border rounded">
                        <div className="font-mono text-sm">{f.path}</div>
                        <pre className="mt-2 p-2 bg-gray-50 text-xs overflow-x-auto">
                          {f.content}
                        </pre>
                      </div>
                    ))}
                </div>
              </TabsContent>

              <TabsContent value="history" className="mt-4">
                <VersionHistory
                  stencilId={stencilId}
                  versions={data.versions}
                  onRestore={handleRestore}
                />
              </TabsContent>
            </div>
          </Tabs>
        </div>
      </div>

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
