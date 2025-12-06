import React, { useState, useEffect } from 'react';
import type { SchDicCode } from '../types/modeler';

interface CodeValueEditorProps {
  groupId: string;
  codes: SchDicCode[];
  onSave: (groupId: string, codes: SchDicCode[]) => void;
  onDeleteGroup: (groupId: string) => void;
}

export const CodeValueEditor: React.FC<CodeValueEditorProps> = ({
  groupId,
  codes: initialCodes,
  onSave,
  onDeleteGroup,
}) => {
  const [codes, setCodes] = useState<SchDicCode[]>(initialCodes);
  const [editingGroupId, setEditingGroupId] = useState(groupId);

  useEffect(() => {
    setCodes(initialCodes);
    setEditingGroupId(groupId);
  }, [initialCodes, groupId]);

  const handleAddCode = () => {
    const newCode: SchDicCode = {
      groupId: editingGroupId || '',
      code: '',
      text: '',
      sort: codes.length + 1,
      deleteFlag: false,
      tenantId: '', // Will be set by backend
      version: 0,
      createdAt: '',
      createdBy: '',
      updatedAt: '',
      updatedBy: '',
    };
    setCodes([...codes, newCode]);
  };

  const handleChange = (index: number, field: keyof SchDicCode, value: any) => {
    const newCodes = [...codes];
    newCodes[index] = { ...newCodes[index], [field]: value } as SchDicCode;
    setCodes(newCodes);
  };

  const handleDelete = (index: number) => {
    const newCodes = codes.filter((_, i) => i !== index);
    setCodes(newCodes);
  };

  const handleSave = () => {
    // Update groupId for all codes if changed
    const updatedCodes = codes.map(c => ({ ...c, groupId: editingGroupId }));
    onSave(editingGroupId, updatedCodes);
  };

  return (
    <div className="flex-1 p-6 bg-background flex flex-col h-full overflow-hidden">
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-4">
          <div>
            <label className="block text-xs text-muted-foreground mb-1">グループID</label>
            <input
              type="text"
              value={editingGroupId}
              onChange={(e) => setEditingGroupId(e.target.value)}
              className="px-3 py-1.5 border border-input rounded bg-background text-foreground text-sm font-medium focus:ring-2 focus:ring-ring"
              readOnly={initialCodes.length > 0} // Read-only if editing existing group
            />
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => onDeleteGroup(groupId)}
            className="px-3 py-1.5 text-sm text-destructive hover:bg-destructive/10 rounded transition-colors"
          >
            グループ削除
          </button>
          <button
            onClick={handleSave}
            className="px-4 py-1.5 text-sm bg-primary text-primary-foreground rounded hover:bg-primary/90 transition-colors shadow-sm"
          >
            保存
          </button>
        </div>
      </div>

      <div className="flex-1 border border-border rounded-lg bg-card overflow-hidden flex flex-col">
        <div className="grid grid-cols-12 gap-4 p-3 border-b border-border bg-muted/50 text-xs font-medium text-muted-foreground">
          <div className="col-span-3">コード値 (Key)</div>
          <div className="col-span-4">表示名 (Label)</div>
          <div className="col-span-2">順序</div>
          <div className="col-span-2">状態</div>
          <div className="col-span-1 text-center">操作</div>
        </div>
        <div className="flex-1 overflow-y-auto p-2 space-y-2">
          {codes.map((code, index) => (
            <div key={index} className="grid grid-cols-12 gap-4 items-center p-2 rounded hover:bg-muted/30 transition-colors">
              <div className="col-span-3">
                <input
                  type="text"
                  value={code.code}
                  onChange={(e) => handleChange(index, 'code', e.target.value)}
                  className="w-full px-2 py-1 border border-input rounded bg-background text-sm"
                  placeholder="key"
                />
              </div>
              <div className="col-span-4">
                <input
                  type="text"
                  value={code.text || ''}
                  onChange={(e) => handleChange(index, 'text', e.target.value)}
                  className="w-full px-2 py-1 border border-input rounded bg-background text-sm"
                  placeholder="Label"
                />
              </div>
              <div className="col-span-2">
                <input
                  type="number"
                  value={code.sort || 0}
                  onChange={(e) => handleChange(index, 'sort', parseInt(e.target.value))}
                  className="w-full px-2 py-1 border border-input rounded bg-background text-sm"
                />
              </div>
              <div className="col-span-2">
                <label className="flex items-center gap-2 text-sm cursor-pointer">
                  <input
                    type="checkbox"
                    checked={!code.deleteFlag}
                    onChange={(e) => handleChange(index, 'deleteFlag', !e.target.checked)}
                    className="rounded border-input"
                  />
                  <span>有効</span>
                </label>
              </div>
              <div className="col-span-1 text-center">
                <button
                  onClick={() => handleDelete(index)}
                  className="text-muted-foreground hover:text-destructive transition-colors"
                >
                  ×
                </button>
              </div>
            </div>
          ))}
          <button
            onClick={handleAddCode}
            className="w-full py-2 border-2 border-dashed border-border rounded text-sm text-muted-foreground hover:border-primary/50 hover:text-primary transition-colors"
          >
            + コード値を追加
          </button>
        </div>
      </div>
    </div>
  );
};
