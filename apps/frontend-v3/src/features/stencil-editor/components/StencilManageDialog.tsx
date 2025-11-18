/**
 * ステンシル管理ダイアログ
 * 1. カテゴリ・ステンシル名編集
 * 2. シリアル改版（丸ごとコピー）
 */
import React, { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Button,
  Input,
  Combobox,
  type ComboboxOption,
  toast,
} from '@mirel/ui';

type ManageMode = 'edit' | 'revision' | null;

interface StencilManageDialogProps {
  stencilId: string;
  stencilName: string;
  categoryId: string;
  currentSerial: string;
  availableCategories: string[];
  availableSerials: string[];
  onSave: (data: {
    categoryId: string;
    stencilName: string;
  }) => Promise<void>;
  onRevision: (data: {
    sourceSerial: string;
    newSerial: string;
  }) => Promise<void>;
  onClose: () => void;
}

export const StencilManageDialog: React.FC<StencilManageDialogProps> = ({
  stencilName: initialStencilName,
  categoryId: initialCategoryId,
  currentSerial,
  availableCategories,
  availableSerials,
  onSave,
  onRevision,
  onClose,
}) => {
  const [mode, setMode] = useState<ManageMode>(null);
  const [loading, setLoading] = useState(false);

  // 編集モード用
  const [categoryId, setCategoryId] = useState(initialCategoryId);
  const [stencilName, setStencilName] = useState(initialStencilName);

  // 改版モード用
  const [sourceSerial, setSourceSerial] = useState(currentSerial);
  const [newSerial, setNewSerial] = useState('');

  const categoryOptions: ComboboxOption[] = availableCategories.map(cat => ({
    value: cat,
    label: cat,
  }));

  const serialOptions: ComboboxOption[] = availableSerials.map(serial => ({
    value: serial,
    label: serial,
  }));

  const handleEditSave = async () => {
    if (!categoryId.trim() || !stencilName.trim()) {
      toast({
        title: '入力エラー',
        description: 'カテゴリとステンシル名を入力してください',
        variant: 'warning',
      });
      return;
    }

    try {
      setLoading(true);
      await onSave({ categoryId, stencilName });
      toast({
        title: '保存完了',
        description: 'ステンシル情報を更新しました',
        variant: 'default',
      });
      onClose();
    } catch (error) {
      console.error('保存エラー:', error);
      toast({
        title: 'エラー',
        description: '保存に失敗しました',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRevisionSave = async () => {
    if (!sourceSerial || !newSerial.trim()) {
      toast({
        title: '入力エラー',
        description: '改版元と新しいシリアル番号を入力してください',
        variant: 'warning',
      });
      return;
    }

    if (availableSerials.includes(newSerial)) {
      toast({
        title: '入力エラー',
        description: 'そのシリアル番号は既に存在します',
        variant: 'warning',
      });
      return;
    }

    const confirmMsg = `シリアル ${sourceSerial} を ${newSerial} にコピーしますか？\nステンシル定義一式がコピーされます。`;
    if (!confirm(confirmMsg)) return;

    try {
      setLoading(true);
      await onRevision({ sourceSerial, newSerial });
      toast({
        title: '改版完了',
        description: `シリアル ${newSerial} を作成しました`,
        variant: 'default',
      });
      onClose();
    } catch (error) {
      console.error('改版エラー:', error);
      toast({
        title: 'エラー',
        description: '改版に失敗しました',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-2xl bg-white">
        <DialogHeader>
          <DialogTitle>ステンシル管理</DialogTitle>
          <div className="text-sm text-gray-500 mt-1">
            現在のシリアル: {currentSerial}
          </div>
        </DialogHeader>

        {!mode && (
          <div className="space-y-4 py-6">
            <div className="text-center text-gray-600 mb-6">
              実行する操作を選択してください
            </div>
            <div className="grid grid-cols-2 gap-4">
              <button
                onClick={() => setMode('edit')}
                className="p-6 border-2 border-gray-300 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-all"
              >
                <div className="text-4xl mb-2">📝</div>
                <div className="font-semibold text-lg mb-1">情報編集</div>
                <div className="text-sm text-gray-600">
                  カテゴリ・ステンシル名を変更
                </div>
              </button>
              <button
                onClick={() => setMode('revision')}
                className="p-6 border-2 border-gray-300 rounded-lg hover:border-green-500 hover:bg-green-50 transition-all"
              >
                <div className="text-4xl mb-2">🔄</div>
                <div className="font-semibold text-lg mb-1">シリアル改版</div>
                <div className="text-sm text-gray-600">
                  新しいシリアルにコピー
                </div>
              </button>
            </div>
            <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg text-sm text-yellow-800">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <span>注意: 編集と改版は同時に実行できません</span>
              </div>
            </div>
          </div>
        )}

        {mode === 'edit' && (
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <label className="text-sm font-semibold text-gray-700">
                カテゴリ <span className="text-red-500">*</span>
              </label>
              <Combobox
                value={categoryId}
                onValueChange={setCategoryId}
                options={categoryOptions}
                placeholder="カテゴリを選択または入力"
                searchPlaceholder="カテゴリを検索..."
                allowCustom={true}
              />
              <p className="text-xs text-gray-500">
                既存のカテゴリから選択、または新しいカテゴリを入力できます
              </p>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-gray-700">
                ステンシル名 <span className="text-red-500">*</span>
              </label>
              <Input
                value={stencilName}
                onChange={(e) => setStencilName(e.target.value)}
                placeholder="ステンシル名を入力"
              />
            </div>
          </div>
        )}

        {mode === 'revision' && (
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <label className="text-sm font-semibold text-gray-700">
                改版元シリアル <span className="text-red-500">*</span>
              </label>
              <Combobox
                value={sourceSerial}
                onValueChange={setSourceSerial}
                options={serialOptions}
                placeholder="改版元を選択"
                searchPlaceholder="シリアル番号を検索..."
                allowCustom={false}
              />
              <p className="text-xs text-gray-500">
                コピー元となるシリアル番号を選択してください
              </p>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-gray-700">
                新しいシリアル番号 <span className="text-red-500">*</span>
              </label>
              <Input
                value={newSerial}
                onChange={(e) => setNewSerial(e.target.value)}
                placeholder="例: 251118A"
              />
              <p className="text-xs text-gray-500">
                新しいシリアル番号を入力してください（既存と重複不可）
              </p>
            </div>

            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-800">
              📋 ステンシル定義一式（YAML + テンプレートファイル）がコピーされます
            </div>
          </div>
        )}

        <DialogFooter>
          {mode && (
            <Button
              variant="ghost"
              onClick={() => setMode(null)}
              disabled={loading}
            >
              ← 戻る
            </Button>
          )}
          {mode === 'edit' && (
            <Button
              onClick={handleEditSave}
              disabled={loading}
            >
              {loading ? '保存中...' : '保存'}
            </Button>
          )}
          {mode === 'revision' && (
            <Button
              onClick={handleRevisionSave}
              disabled={loading}
            >
              {loading ? '改版中...' : '改版実行'}
            </Button>
          )}
          {!mode && (
            <Button variant="ghost" onClick={onClose}>
              閉じる
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
