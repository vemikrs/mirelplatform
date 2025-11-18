/**
 * バージョン履歴ダイアログ
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
  stencilId: string;  // 将来の拡張用
  currentSerial: string;
  versions: StencilVersion[];
  onRestore: (serial: string) => void;
  onClose: () => void;
}

export const HistoryDialog: React.FC<HistoryDialogProps> = ({
  stencilId: _stencilId,  // eslint-disable-line @typescript-eslint/no-unused-vars
  currentSerial,
  versions,
  onRestore,
  onClose,
}) => {
  const [selectedVersion, setSelectedVersion] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleRestore = async () => {
    if (!selectedVersion) return;

    try {
      setLoading(true);
      await onRestore(selectedVersion);
      onClose();
      
      toast({
        title: 'バージョン復元',
        description: `バージョン ${selectedVersion} を復元しました`,
        variant: 'success',
      });
    } catch (error) {
      console.error('復元エラー:', error);
      toast({
        title: '復元エラー',
        description: 'バージョンの復元に失敗しました',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-3xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>バージョン履歴</DialogTitle>
        </DialogHeader>

        <div className="space-y-2 max-h-96 overflow-y-auto p-4">
          {versions.map((version) => {
            const isCurrent = version.serial === currentSerial;
            const isSelected = version.serial === selectedVersion;

            return (
              <div
                key={version.serial}
                className={`
                  p-4 border rounded-lg cursor-pointer transition-all
                  ${isCurrent 
                    ? 'border-blue-500 bg-blue-50' 
                    : isSelected
                    ? 'border-gray-400 bg-gray-50'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  }
                `}
                onClick={() => setSelectedVersion(version.serial)}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-lg">
                        {version.serial}
                      </span>
                      {isCurrent && (
                        <span className="px-2 py-0.5 text-xs bg-blue-500 text-white rounded-full">
                          現在
                        </span>
                      )}
                    </div>
                    
                    <div className="mt-1 text-sm text-gray-600">
                      {version.updateDate}
                    </div>
                    
                    <div className="mt-1 text-xs text-gray-500">
                      更新者: {version.updateUser}
                    </div>

                    {version.description && (
                      <div className="mt-2 text-sm text-gray-700">
                        {version.description}
                      </div>
                    )}
                  </div>

                  {isSelected && !isCurrent && (
                    <div className="ml-4">
                      <Button
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRestore();
                        }}
                        disabled={loading}
                      >
                        復元
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          {versions.length === 0 && (
            <div className="text-center py-8 text-gray-500">
              バージョン履歴がありません
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={onClose}>
            閉じる
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
