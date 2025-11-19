/**
 * バージョン履歴ダイアログ
 * シリアル改版管理と自動バックアップの両方を扱う
 */
import React, { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Button,
  toast,
} from '@mirel/ui';
import type { StencilVersion } from '../types';

interface HistoryDialogProps {
  stencilId: string;
  currentSerial: string;
  versions: StencilVersion[];
  onRestore: (serial: string) => void;
  onDelete?: (serial: string) => void;
  onShowDiff?: (oldSerial: string, newSerial: string) => void;
  onClose: () => void;
}

export const HistoryDialog: React.FC<HistoryDialogProps> = ({
  currentSerial,
  versions,
  onRestore,
  onDelete,
  onShowDiff,
  onClose,
}) => {
  const [selectedVersion, setSelectedVersion] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleRestore = async () => {
    if (!selectedVersion) return;

    const version = versions.find(v => v.serial === selectedVersion);
    if (!version) return;

    const confirmMsg = `バージョン ${selectedVersion} (${version.updateDate}) に復元しますか？\n現在の変更は失われます。`;
    if (!confirm(confirmMsg)) return;

    try {
      setLoading(true);
      await onRestore(selectedVersion);
      onClose();
      
      toast({
        title: '復元完了',
        description: `バージョン ${selectedVersion} を復元しました`,
        variant: 'default',
      });
    } catch (error) {
      console.error('復元エラー:', error);
      toast({
        title: 'エラー',
        description: 'バージョンの復元に失敗しました',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (serial: string) => {
    if (serial === currentSerial) {
      toast({
        title: '削除不可',
        description: '現在使用中のバージョンは削除できません',
        variant: 'warning',
      });
      return;
    }

    const version = versions.find(v => v.serial === serial);
    if (!version) return;

    const confirmMsg = `バージョン ${serial} (${version.updateDate}) を削除しますか？\nこの操作は取り消せません。`;
    if (!confirm(confirmMsg)) return;

    try {
      setLoading(true);
      await onDelete?.(serial);
      
      toast({
        title: '削除完了',
        description: `バージョン ${serial} を削除しました`,
        variant: 'default',
      });
    } catch (error) {
      console.error('削除エラー:', error);
      toast({
        title: 'エラー',
        description: 'バージョンの削除に失敗しました',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleShowDiff = () => {
    if (!selectedVersion || selectedVersion === currentSerial) {
      toast({
        title: '差分表示不可',
        description: '異なるバージョンを選択してください',
        variant: 'warning',
      });
      return;
    }

    onShowDiff?.(currentSerial, selectedVersion);
    setShowDiff(true);
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[85vh] bg-surface">
        <DialogHeader>
          <DialogTitle>バージョン履歴</DialogTitle>
          <div className="text-sm text-muted-foreground mt-1">
            現在: {currentSerial}
          </div>
        </DialogHeader>

        <div className="grid grid-cols-1 gap-4">
          {/* バージョンリスト */}
          <div className="space-y-2 max-h-[500px] overflow-y-auto border border-border rounded-lg p-4 bg-surface-subtle">
            {versions.map((version) => {
              const isCurrent = version.serial === currentSerial;
              const isSelected = version.serial === selectedVersion;

              return (
                <div
                  key={version.serial}
                  className={`
                    p-4 border rounded-lg cursor-pointer transition-all
                    ${isCurrent 
                      ? 'border-primary bg-primary/10 shadow-sm' 
                      : isSelected
                      ? 'border-green-400 bg-green-50 dark:bg-green-900/30 shadow-sm'
                      : 'border-gray-200 border-border bg-surface hover:border-border hover:shadow'
                    }
                  `}
                  onClick={() => setSelectedVersion(version.serial)}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      {/* シリアル番号 */}
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-lg font-bold text-foreground">
                          {version.serial}
                        </span>
                        {isCurrent && (
                          <span className="px-2 py-0.5 text-xs bg-primary text-primary-foreground rounded font-semibold">
                            ● 現在使用中
                          </span>
                        )}
                        {isSelected && !isCurrent && (
                          <span className="flex items-center gap-1 px-2 py-0.5 text-xs bg-green-600 text-white rounded font-semibold">
                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                            選択中
                          </span>
                        )}
                      </div>
                      
                      {/* 更新日時・更新者 */}
                      <div className="space-y-1 text-sm text-foreground">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-muted-foreground">更新日時:</span>
                          <span>{version.updateDate || '不明'}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-muted-foreground">更新者:</span>
                          <span>{version.updateUser || '不明'}</span>
                        </div>
                      </div>

                      {/* 説明 */}
                      {version.description && (
                        <div className="mt-2 text-sm text-foreground bg-surface p-2 rounded border border-gray-200 border-border">
                          {version.description}
                        </div>
                      )}
                    </div>

                    {/* アクションボタン */}
                    {!isCurrent && (
                      <div className="ml-4 flex flex-col gap-2">
                        <Button
                          size="sm"
                          variant={isSelected ? 'default' : 'outline'}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleRestore();
                          }}
                          disabled={loading || !isSelected}
                          className="min-w-[80px]"
                        >
                          復元
                        </Button>
                        {onDelete && (
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDelete(version.serial);
                            }}
                            disabled={loading}
                            className="min-w-[80px]"
                          >
                            削除
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}

            {versions.length === 0 && (
              <div className="text-center py-12 text-muted-foreground">
                <div className="text-4xl mb-2">📋</div>
                <div>バージョン履歴がありません</div>
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <div className="flex w-full justify-between">
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={handleShowDiff}
                disabled={!selectedVersion || selectedVersion === currentSerial || loading}
              >
                📊 差分表示
              </Button>
            </div>
            <div className="flex gap-2">
              <Button variant="ghost" onClick={onClose}>
                閉じる
              </Button>
            </div>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
