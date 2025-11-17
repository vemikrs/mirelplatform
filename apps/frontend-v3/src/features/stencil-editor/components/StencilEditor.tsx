/**
 * メインのステンシルエディタコンポーネント
 */
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { YamlEditor } from './YamlEditor';
import { loadStencil, saveStencil } from '../api/stencil-editor-api';
import type { LoadStencilResponse, EditorMode, StencilFile } from '../types';
import { Button } from '@mirel/ui';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@mirel/ui';

export const StencilEditor: React.FC = () => {
  const { stencilId, serial } = useParams<{ stencilId: string; serial: string }>();
  const navigate = useNavigate();
  
  const [mode, setMode] = useState<EditorMode>('view');
  const [data, setData] = useState<LoadStencilResponse | null>(null);
  const [yamlContent, setYamlContent] = useState('');
  const [activeTab, setActiveTab] = useState('yaml');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // データ読込
  useEffect(() => {
    if (stencilId && serial) {
      loadData(stencilId, serial);
    }
  }, [stencilId, serial]);

  const loadData = async (id: string, ser: string) => {
    setLoading(true);
    try {
      const result = await loadStencil(id, ser);
      setData(result);
      
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

  const handleSave = async () => {
    if (!data || !stencilId || !serial) return;

    setSaving(true);
    try {
      // ファイルリストを更新
      const updatedFiles = data.files.map(f => {
        if (f.name === 'stencil-settings.yml') {
          return { ...f, content: yamlContent };
        }
        return f;
      });

      await saveStencil({
        stencilId,
        serial,
        config: data.config,
        files: updatedFiles,
        message: '編集保存',
      });

      alert('保存しました');
      // 一覧へ戻る
      navigate('/promarker/stencils');
    } catch (error) {
      console.error('保存エラー:', error);
      alert('保存に失敗しました');
    } finally {
      setSaving(false);
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
        <h1 className="text-2xl font-bold">{data.config.name}</h1>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => setMode(mode === 'view' ? 'edit' : 'view')}
          >
            {mode === 'view' ? '編集モード' : '参照モード'}
          </Button>
          {mode === 'edit' && (
            <Button onClick={handleSave} disabled={saving}>
              {saving ? '保存中...' : '保存'}
            </Button>
          )}
          <Button variant="outline" onClick={() => navigate('/promarker/stencils')}>
            一覧へ戻る
          </Button>
        </div>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="yaml">YAML設定</TabsTrigger>
          <TabsTrigger value="templates">テンプレート</TabsTrigger>
          <TabsTrigger value="files">その他ファイル</TabsTrigger>
          <TabsTrigger value="history">履歴</TabsTrigger>
        </TabsList>

        <TabsContent value="yaml" className="mt-4">
          <YamlEditor
            value={yamlContent}
            onChange={setYamlContent}
            readOnly={mode === 'view'}
          />
        </TabsContent>

        <TabsContent value="templates" className="mt-4">
          <div className="space-y-2">
            {data.files
              .filter(f => f.type === 'template')
              .map(f => (
                <div key={f.path} className="p-2 border rounded">
                  <div className="font-mono text-sm">{f.path}</div>
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
          <div className="space-y-2">
            {data.versions.map(v => (
              <div key={v.serial} className="p-2 border rounded">
                <div className="font-mono">{v.serial}</div>
                <div className="text-sm text-gray-600">{v.createdAt}</div>
              </div>
            ))}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
};
