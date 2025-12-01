import React from 'react';

interface CodeGroupListProps {
  groups: string[];
  selectedGroupId: string | null;
  onSelect: (groupId: string) => void;
  onCreate: () => void;
}

export const CodeGroupList: React.FC<CodeGroupListProps> = ({
  groups,
  selectedGroupId,
  onSelect,
  onCreate,
}) => {
  return (
    <div className="w-64 border-r border-border bg-card flex flex-col">
      <div className="p-4 border-b border-border flex justify-between items-center">
        <h2 className="font-semibold text-foreground">コードグループ</h2>
        <button
          onClick={onCreate}
          className="text-xs bg-primary text-primary-foreground px-2 py-1 rounded hover:bg-primary/90 transition-colors"
        >
          + 新規
        </button>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-1">
        {groups.map((group) => (
          <button
            key={group}
            onClick={() => onSelect(group)}
            className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${
              selectedGroupId === group
                ? 'bg-primary/10 text-primary font-medium'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            }`}
          >
            {group}
          </button>
        ))}
        {groups.length === 0 && (
          <div className="text-xs text-muted-foreground text-center py-4">
            グループがありません
          </div>
        )}
      </div>
    </div>
  );
};
