/**
 * バージョン履歴コンポーネント
 */
import React, { useState, useEffect } from 'react';
import { Button, toast } from '@mirel/ui';
import { DiffViewer } from './DiffViewer';
import { getVersionHistory, loadStencil } from '../api/stencil-editor-api';
import type { StencilVersion, StencilFile } from '../types';

interface VersionHistoryProps {
  stencilId: string;
  currentSerial: string;
  onRestore?: (serial: string) => void;
}

export const VersionHistory: React.FC<VersionHistoryProps> = ({
  stencilId,
  currentSerial,
  onRestore,
}) => {
  const [versions, setVersions] = useState<StencilVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [compareMode, setCompareMode] = useState(false);
  const [oldVersion, setOldVersion] = useState<string | null>(null);
  const [newVersion, setNewVersion] = useState<string | null>(null);
  const [diffData, setDiffData] = useState<{
    oldFiles: StencilFile[];
    newFiles: StencilFile[];
  } | null>(null);

  // バージョン一覧読込
  useEffect(() => {
    loadVersions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadVersions = async () => {
    setLoading(true);
    try {
      const result = await getVersionHistory(stencilId);
      setVersions(result);
    } catch (error) {
      console.error('バージョン履歴の読込エラー:', error);
      toast({
        title: '読み込みエラー',
        description: 'バージョン履歴の読み込みに失敗しました',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  // 差分表示
  const handleCompare = async () => {
    if (!oldVersion || !newVersion) {
      alert('比較するバージョンを2つ選択してください');
      return;
    }

    try {
      const [oldData, newData] = await Promise.all([
        loadStencil(stencilId, oldVersion),
        loadStencil(stencilId, newVersion),
      ]);

      setDiffData({
        oldFiles: oldData.files,
        newFiles: newData.files,
      });
      setCompareMode(true);
    } catch (error) {
      console.error('差分表示エラー:', error);
      toast({
        title: '差分表示エラー',
        description: '差分表示に失敗しました',
        variant: 'destructive',
      });
    }
  };

  // 復元処理
  const handleRestore = (serial: string) => {
    const confirmRestore = window.confirm(
      `バージョン ${serial} に復元しますか？\n現在の変更は新しいバージョンとして保存されます。`
    );

    if (confirmRestore && onRestore) {
      onRestore(serial);
    }
  };

  // 日時フォーマット
  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    try {
      const date = new Date(dateString);
      return date.toLocaleString('ja-JP', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateString;
    }
  };

  if (loading) {
    return <div className="p-4">読み込み中...</div>;
  }

  // 差分表示モード
  if (compareMode && diffData) {
    return (
      <div className="version-diff-view">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold">
            差分表示: {oldVersion} → {newVersion}
          </h3>
          <Button variant="outline" onClick={() => setCompareMode(false)}>
            履歴に戻る
          </Button>
        </div>

        <div className="space-y-6">
          {/* stencil-settings.yml の差分 */}
          {(() => {
            const oldFile = diffData.oldFiles.find(
              (f) => f.name === 'stencil-settings.yml'
            );
            const newFile = diffData.newFiles.find(
              (f) => f.name === 'stencil-settings.yml'
            );

            if (oldFile && newFile) {
              return (
                <div>
                  <h4 className="mb-2 font-semibold">stencil-settings.yml</h4>
                  <DiffViewer
                    oldValue={oldFile.content}
                    newValue={newFile.content}
                    oldTitle={`${oldVersion}`}
                    newTitle={`${newVersion}`}
                    language="yaml"
                  />
                </div>
              );
            }
            return null;
          })()}

          {/* テンプレートファイルの差分 */}
          {diffData.newFiles
            .filter((f) => f.type === 'template')
            .map((newFile) => {
              const oldFile = diffData.oldFiles.find(
                (f) => f.path === newFile.path
              );

              return (
                <div key={newFile.path}>
                  <h4 className="mb-2 font-semibold">{newFile.path}</h4>
                  <DiffViewer
                    oldValue={oldFile?.content || ''}
                    newValue={newFile.content}
                    oldTitle={`${oldVersion}`}
                    newTitle={`${newVersion}`}
                    language={newFile.language || 'text'}
                  />
                </div>
              );
            })}
        </div>
      </div>
    );
  }

  // バージョン一覧表示
  return (
    <div className="version-history">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-semibold">バージョン履歴</h3>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={handleCompare}
            disabled={!oldVersion || !newVersion}
          >
            差分表示
          </Button>
        </div>
      </div>

      {/* 比較用バージョン選択ヘルプ */}
      <div className="mb-4 rounded bg-primary/10 p-3 text-sm text-primary">
        <p>
          💡 2つのバージョンを選択して「差分表示」ボタンをクリックすると、変更内容を確認できます
        </p>
      </div>

      {/* バージョンリスト */}
      <div className="space-y-2">
        {versions.map((version) => {
          const isCurrent = version.serial === currentSerial;
          const isOldSelected = oldVersion === version.serial;
          const isNewSelected = newVersion === version.serial;

          return (
            <div
              key={version.serial}
              className={`rounded border p-4 ${
                isCurrent
                  ? 'border-primary bg-primary/10'
                  : 'border-border bg-surface'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-semibold">
                      {version.serial}
                    </span>
                    {isCurrent && (
                      <span className="rounded bg-primary px-2 py-1 text-xs text-primary-foreground">
                        現在
                      </span>
                    )}
                    {version.isActive && !isCurrent && (
                      <span className="rounded bg-green-600 dark:bg-green-700 px-2 py-1 text-xs text-white">
                        有効
                      </span>
                    )}
                  </div>
                  <div className="mt-1 text-sm text-muted-foreground">
                    {formatDate(version.createdAt)} - {version.createdBy || '不明'}
                  </div>
                  {version.changes && (
                    <div className="mt-1 text-sm text-foreground">
                      {version.changes}
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  {/* 比較用チェックボックス */}
                  <div className="flex flex-col gap-1">
                    <label className="flex items-center gap-1 text-xs">
                      <input
                        type="radio"
                        name="oldVersion"
                        checked={isOldSelected}
                        onChange={() => setOldVersion(version.serial)}
                      />
                      旧
                    </label>
                    <label className="flex items-center gap-1 text-xs">
                      <input
                        type="radio"
                        name="newVersion"
                        checked={isNewSelected}
                        onChange={() => setNewVersion(version.serial)}
                      />
                      新
                    </label>
                  </div>

                  {/* 復元ボタン */}
                  {!isCurrent && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleRestore(version.serial)}
                    >
                      復元
                    </Button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {versions.length === 0 && (
        <div className="rounded border border-border bg-surface-subtle p-8 text-center text-muted-foreground">
          バージョン履歴がありません
        </div>
      )}
    </div>
  );
};
