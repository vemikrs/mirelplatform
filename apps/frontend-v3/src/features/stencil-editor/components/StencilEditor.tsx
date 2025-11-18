/**
 * メインのステンシルエディタコンポーネント
 */
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { YamlEditor } from './YamlEditor';
import type { YamlEditorHandle } from './YamlEditor';
import { TemplateEditor } from './TemplateEditor';
import type { TemplateEditorHandle } from './TemplateEditor';
import { ErrorPanel } from './ErrorPanel';
import type { ValidationError } from './ErrorPanel';
import { PreviewPanel } from './PreviewPanel';
import { FileExplorer } from './FileExplorer';
import { FileTabs } from './FileTabs';
import type { OpenTab } from './FileTabs';
import { HistoryDialog } from './HistoryDialog';
import { Button, toast } from '@mirel/ui';
import { loadStencil, saveStencil } from '../api/stencil-editor-api';
import type { LoadStencilResponse, EditorMode } from '../types';

export const StencilEditor: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const location = window.location.pathname;
  
  // URLから stencilId と serial を抽出（例: /promarker/editor/springboot/service/221208A）
  const pathParts = location.split('/').filter(Boolean);
  const editorIndex = pathParts.indexOf('editor');
  
  // editor以降のパスを解析
  // 最後の要素がシリアル番号（例: 221208A）
  const serial = pathParts[pathParts.length - 1];
  // editor の次から最後の1つ前までがstencilId（例: /springboot/service）
  const stencilId = '/' + pathParts.slice(editorIndex + 1, -1).join('/');
  
  console.log('🔍 StencilEditor URL解析:', {
    location,
    pathParts,
    editorIndex,
    stencilId,
    serial,
  });
  
  // クエリパラメータからmodeを取得(デフォルトはview)
  const initialMode = (searchParams.get('mode') as EditorMode) || 'view';
  const [mode, setMode] = useState<EditorMode>(initialMode);
  const [data, setData] = useState<LoadStencilResponse | null>(null);
  const [yamlContent, setYamlContent] = useState('');
  const [templateContents, setTemplateContents] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [validationErrors, setValidationErrors] = useState<ValidationError[]>([]);
  const [autoSaveEnabled, setAutoSaveEnabled] = useState(true);
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [showHistoryDialog, setShowHistoryDialog] = useState(false);
  const [explorerCollapsed, setExplorerCollapsed] = useState(false);
  
  // タブ管理
  const [openTabs, setOpenTabs] = useState<OpenTab[]>([]);
  const [activeTabPath, setActiveTabPath] = useState<string | null>(null);
  
  const autoSaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  
  const yamlEditorRef = useRef<YamlEditorHandle>(null);
  const templateEditorRefs = useRef<Record<string, TemplateEditorHandle>>({});

  // データ読込
  useEffect(() => {
    if (stencilId && serial) {
      loadData(stencilId, serial);
    }
  }, [stencilId, serial]);

  // 初期タブを自動で開く
  useEffect(() => {
    if (data && openTabs.length === 0) {
      const yamlFile = data.files.find(f => f.name === 'stencil-settings.yml');
      if (yamlFile) {
        // 直接タブを開く（handleFileOpenの定義前なので）
        setOpenTabs([{
          path: yamlFile.path,
          name: yamlFile.name,
          type: yamlFile.type,
          isDirty: false
        }]);
        setActiveTabPath(yamlFile.path);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

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

  const handleErrorClick = useCallback((error: ValidationError) => {
    // エラー位置にスクロール
    if (!data) return;
    
    // エラーファイルを見つけてタブを開く
    const errorFile = data.files.find(f => f.name === error.file || f.path === error.file);
    if (!errorFile) return;
    
    // タブが開いていなければ開く
    const existingTab = openTabs.find(t => t.path === errorFile.path);
    if (!existingTab) {
      setOpenTabs(prev => [...prev, {
        path: errorFile.path,
        name: errorFile.name,
        type: errorFile.type,
        isDirty: false
      }]);
    }
    
    // アクティブにする
    setActiveTabPath(errorFile.path);
    
    // エディタにスクロール
    if (error.file === 'stencil-settings.yml' && error.line && yamlEditorRef.current) {
      setTimeout(() => yamlEditorRef.current?.scrollToLine(error.line!), 100);
    } else if (error.file && templateEditorRefs.current[errorFile.path] && error.line) {
      setTimeout(() => templateEditorRefs.current[errorFile.path]?.scrollToLine(error.line!), 100);
    }
  }, [data, openTabs, setOpenTabs, setActiveTabPath]);

  // ファイル選択ハンドラー（タブを開く）
  const handleFileOpen = useCallback((file: typeof data.files[0]) => {
    const existingTab = openTabs.find(t => t.path === file.path);
    
    if (existingTab) {
      // 既に開いている → フォーカス
      setActiveTabPath(file.path);
    } else {
      // 新規タブを追加
      setOpenTabs(prev => [...prev, {
        path: file.path,
        name: file.name,
        type: file.type,
        isDirty: false
      }]);
      setActiveTabPath(file.path);
    }
  }, [openTabs, setOpenTabs, setActiveTabPath]);

  // タブを閉じる
  const handleTabClose = (path: string) => {
    const tab = openTabs.find(t => t.path === path);
    
    if (tab?.isDirty) {
      // 未保存警告
      if (!confirm(`${tab.name} に未保存の変更があります。閉じますか？`)) {
        return;
      }
    }
    
    setOpenTabs(prev => prev.filter(t => t.path !== path));
    
    // アクティブタブを閉じた場合、次のタブに移動
    if (activeTabPath === path) {
      const remainingTabs = openTabs.filter(t => t.path !== path);
      setActiveTabPath(remainingTabs[0]?.path || null);
    }
  };

  // 内容変更時にdirtyフラグを更新
  const handleContentChange = (path: string, content: string) => {
    setOpenTabs(prev => prev.map(tab => 
      tab.path === path 
        ? { ...tab, isDirty: true }
        : tab
    ));
    
    // 実際のコンテンツ保存
    if (path.endsWith('stencil-settings.yml')) {
      setYamlContent(content);
    } else {
      setTemplateContents(prev => ({ ...prev, [path]: content }));
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
        
        // YAMLファイルのタブを開く
        const existingYamlTab = openTabs.find(t => t.path === settingsFile.path);
        if (!existingYamlTab) {
          setOpenTabs(prev => [...prev, {
            path: settingsFile.path,
            name: settingsFile.name,
            type: settingsFile.type,
            isDirty: true  // 復元後は未保存状態
          }]);
        } else {
          // 既存タブをdirtyに
          setOpenTabs(prev => prev.map(t => 
            t.path === settingsFile.path ? { ...t, isDirty: true } : t
          ));
        }
        setActiveTabPath(settingsFile.path);
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

  // アクティブなエディタをレンダリング
  const renderActiveEditor = () => {
    if (!activeTabPath) {
      return (
        <div className="flex items-center justify-center h-full text-gray-500">
          ← ファイルを選択してください
        </div>
      );
    }

    const activeFile = data.files.find(f => f.path === activeTabPath);
    if (!activeFile) return null;

    // YAML エディタ
    if (activeFile.name === 'stencil-settings.yml') {
      return (
        <div className="p-4">
          <YamlEditor
            ref={yamlEditorRef}
            value={yamlContent}
            onChange={(content) => handleContentChange(activeFile.path, content)}
            onValidationChange={(errors) => {
              setValidationErrors(prev => [
                ...prev.filter(e => e.file !== 'stencil-settings.yml'),
                ...errors
              ]);
            }}
            readOnly={mode === 'view'}
          />
        </div>
      );
    }

    // テンプレートエディタ
    if (activeFile.type === 'template') {
      return (
        <div className="p-4">
          <TemplateEditor
            ref={el => {
              if (el) templateEditorRefs.current[activeFile.path] = el;
            }}
            value={templateContents[activeFile.path] || activeFile.content}
            onChange={(content) => handleContentChange(activeFile.path, content)}
            fileName={activeFile.name}
            onValidationChange={(errors) => {
              setValidationErrors(prev => [
                ...prev.filter(e => e.file !== activeFile.path),
                ...errors
              ]);
            }}
            readOnly={mode === 'view'}
          />
        </div>
      );
    }

    // その他のファイル（読み取り専用）
    return (
      <div className="p-4">
        <div className="mb-2 font-mono text-sm font-semibold">{activeFile.path}</div>
        <pre className="p-4 bg-gray-50 text-xs overflow-x-auto rounded border">
          {activeFile.content}
        </pre>
      </div>
    );
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
          <Button 
            variant="outline" 
            onClick={() => setShowHistoryDialog(true)}
            title="バージョン履歴"
          >
            📋 履歴
          </Button>
          <Button variant="outline" onClick={() => navigate('/promarker/stencils')}>
            一覧へ戻る
          </Button>
        </div>
      </div>

      {/* エラーパネル */}
      <ErrorPanel errors={validationErrors} onErrorClick={handleErrorClick} />

      {/* メインレイアウト: エクスプローラー + タブ */}
      <div className="flex gap-0 h-[calc(100vh-280px)] relative">
        {/* 左側: ファイルエクスプローラー */}
        <div 
          className="border-r bg-gray-50 overflow-hidden flex-shrink-0 transition-all duration-300 relative"
          style={{ width: explorerCollapsed ? '0px' : '30%' }}
        >
          {!explorerCollapsed && (
            <>
              {/* 折りたたみボタン */}
              <button
                onClick={() => setExplorerCollapsed(true)}
                className="absolute top-2 right-2 z-10 p-1 bg-white border border-gray-300 rounded hover:bg-gray-100 transition-colors"
                title="エクスプローラーを閉じる"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              <FileExplorer
                files={data.files}
                currentFilePath={activeTabPath}
                onFileSelect={handleFileOpen}
                onFileRename={handleFileRename}
                onFileCreate={handleFileCreate}
                onFileDelete={handleFileDelete}
                readOnly={mode === 'view'}
              />
            </>
          )}
        </div>

        {/* 展開ボタン（折りたたみ時のみ表示） */}
        {explorerCollapsed && (
          <button
            onClick={() => setExplorerCollapsed(false)}
            className="absolute left-0 top-2 z-20 p-2 bg-white border border-gray-300 rounded-r hover:bg-gray-100 transition-colors shadow-md"
            title="エクスプローラーを開く"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        )}

        {/* 右側: タブ + エディタエリア */}
        <div className="flex-1 overflow-hidden flex flex-col" style={{ width: explorerCollapsed ? '100%' : '70%' }}>
          {/* タブバー */}
          <FileTabs
            tabs={openTabs}
            activeTab={activeTabPath}
            onTabChange={setActiveTabPath}
            onTabClose={handleTabClose}
          />

          {/* エディタエリア */}
          <div className="flex-1 overflow-auto">
            {renderActiveEditor()}
          </div>
        </div>
      </div>

      {/* 履歴ダイアログ */}
      {showHistoryDialog && (
        <HistoryDialog
          stencilId={stencilId}
          currentSerial={data.config.serial}
          versions={data.versions}
          onRestore={handleRestore}
          onClose={() => setShowHistoryDialog(false)}
        />
      )}

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
