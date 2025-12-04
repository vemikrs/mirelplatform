import { useState, useEffect } from 'react';
import { modelerApi } from '../api/modelerApi';
import type { SchDicCode } from '../types/modeler';
import { CodeGroupList } from '../components/CodeGroupList';
import { CodeValueEditor } from '../components/CodeValueEditor';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';

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
    <StudioLayout showHeader={true}>
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
            { label: 'コードマスタ' },
          ]}
          title="コードマスタ管理"
        />
        
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
    </StudioLayout>
  );
};
