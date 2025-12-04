import React, { useState, useEffect } from 'react';
import { modelerApi } from '../api/modelerApi';
import type { SchDicCode } from '../types/modeler';
import { CodeGroupList } from '../components/CodeGroupList';
import { CodeValueEditor } from '../components/CodeValueEditor';
import { ModelerLayout } from '../components/layout/ModelerLayout';

export const ModelerCodeMasterPage: React.FC = () => {
  const [groups, setGroups] = useState<string[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(null);
  const [codes, setCodes] = useState<SchDicCode[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadGroups();
  }, []);

  useEffect(() => {
    if (selectedGroupId) {
      if (selectedGroupId === 'new_group') {
        setCodes([]);
      } else {
        loadCodes(selectedGroupId);
      }
    } else {
      setCodes([]);
    }
  }, [selectedGroupId]);

  const loadGroups = async () => {
    try {
      const res = await modelerApi.listCodeGroups();
      setGroups(res.data.groups);
      if (res.data.groups.length > 0 && !selectedGroupId) {
        setSelectedGroupId(res.data.groups[0]);
      }
    } catch (error) {
      console.error('Failed to load groups:', error);
    }
  };

  const loadCodes = async (groupId: string) => {
    try {
      setLoading(true);
      const res = await modelerApi.listCode(groupId);
      setCodes(res.data.valueTexts);
    } catch (error) {
      console.error('Failed to load codes:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateGroup = () => {
    setSelectedGroupId('new_group');
    setCodes([]);
  };

  const handleSave = async (groupId: string, updatedCodes: SchDicCode[]) => {
    try {
      await modelerApi.saveCode(groupId, updatedCodes);
      alert('保存しました');
      await loadGroups();
      setSelectedGroupId(groupId);
    } catch (error) {
      console.error('Failed to save codes:', error);
      alert('保存に失敗しました');
    }
  };

  const handleDeleteGroup = async (groupId: string) => {
    if (!confirm('このグループを削除してもよろしいですか？')) return;
    try {
      await modelerApi.deleteCode(groupId);
      alert('削除しました');
      await loadGroups();
      setSelectedGroupId(null);
    } catch (error) {
      console.error('Failed to delete group:', error);
      alert('削除に失敗しました');
    }
  };

  return (
    <ModelerLayout>
      <div className="flex flex-col h-full">
        <div className="flex items-center gap-4 p-4 border-b border-border bg-background">
          <h1 className="text-xl font-bold text-foreground">コードマスタ管理</h1>
        </div>
        
        <div className="flex flex-1 overflow-hidden">
          <CodeGroupList
            groups={groups}
            selectedGroupId={selectedGroupId}
            onSelect={setSelectedGroupId}
            onCreate={handleCreateGroup}
          />
          
          {loading ? (
            <div className="flex-1 flex items-center justify-center text-muted-foreground">
              読み込み中...
            </div>
          ) : selectedGroupId ? (
            <CodeValueEditor
              groupId={selectedGroupId === 'new_group' ? '' : selectedGroupId}
              codes={codes}
              onSave={handleSave}
              onDeleteGroup={handleDeleteGroup}
            />
          ) : (
            <div className="flex-1 flex items-center justify-center text-muted-foreground">
              左側のリストからグループを選択するか、新規作成してください
            </div>
          )}
        </div>
      </div>
    </ModelerLayout>
  );
};
