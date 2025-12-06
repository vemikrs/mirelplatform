import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { modelerApi } from '../api/modelerApi';
import type { SchDicCode } from '../types/modeler';
import { CodeGroupList } from '../components/CodeGroupList';
import { CodeValueEditor } from '../components/CodeValueEditor';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';
import { StudioNavigation } from '../../components/StudioNavigation';
import { ModelerTree } from '../components/ModelerTree';

export const ModelerCodeMasterPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryGroupId = searchParams.get('id');

  const [groups, setGroups] = useState<string[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(queryGroupId);
  const [codes, setCodes] = useState<SchDicCode[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (queryGroupId) {
      setSelectedGroupId(queryGroupId);
    }
  }, [queryGroupId]);

  const handleGroupSelect = (groupId: string) => {
      setSelectedGroupId(groupId);
      setSearchParams({ id: groupId });
  };

  const loadGroups = useCallback(async () => {
    try {
      const res = await modelerApi.listCodeGroups();
      setGroups(res.data.groups);
      if (res.data.groups.length > 0 && !selectedGroupId && !queryGroupId) {
        // Only select first if nothing selected
        // handleGroupSelect(res.data.groups[0]); // triggering navigation might be annoying on load
        setSelectedGroupId(res.data.groups[0]);
      }
    } catch (error) {
      console.error('Failed to load groups:', error);
    }
  }, [selectedGroupId, queryGroupId]);

  const loadCodes = useCallback(async (groupId: string) => {
    try {
      setLoading(true);
      const res = await modelerApi.listCode(groupId);
      setCodes(res.data.valueTexts);
    } catch (error) {
      console.error('Failed to load codes:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadGroups();
  }, [loadGroups]);

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
  }, [selectedGroupId, loadCodes]);

  const handleCreateGroup = () => {
    setSelectedGroupId('new_group');
    setSearchParams({ id: 'new_group' });
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
    <StudioLayout
      navigation={<StudioNavigation className="h-auto shrink-0 max-h-[40%] border-b" />}
      explorer={<ModelerTree className="flex-1" />}
      hideContextBar={true}
    >
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
            onSelect={handleGroupSelect}
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
