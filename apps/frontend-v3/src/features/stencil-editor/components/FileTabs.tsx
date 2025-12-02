/**
 * ファイルタブコンポーネント（VS Code風）
 */
import React from 'react';
import type { FileType } from '../types';

interface OpenTab {
  path: string;
  name: string;
  type: FileType;
  isDirty: boolean;
}

interface FileTabsProps {
  tabs: OpenTab[];
  activeTab: string | null;
  onTabChange: (path: string) => void;
  onTabClose: (path: string) => void;
}

  const getFileIcon = () => {
    return (
      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    );
  };

export const FileTabs: React.FC<FileTabsProps> = ({
  tabs,
  activeTab,
  onTabChange,
  onTabClose,
}) => {
  if (tabs.length === 0) {
    return (
      <div className="h-7 border-b border-border flex items-center px-2 bg-surface-subtle text-xs text-muted-foreground">
        ← ファイルを選択してください
      </div>
    );
  }

  return (
    <div 
      className="flex border-b border-border bg-surface-subtle"
      style={{
        overflowX: 'auto',
        overflowY: 'hidden',
        scrollbarWidth: 'thin', // Firefox
        WebkitOverflowScrolling: 'touch', // iOS
      }}
    >
      {tabs.map((tab) => {
        const isActive = activeTab === tab.path;
        
        return (
          <div
            key={tab.path}
            className={`
              group flex items-center gap-1 px-2 py-1 min-w-fit cursor-pointer border-r border-gray-200 border-border
              transition-colors select-none
              ${isActive 
                ? 'bg-background border-b-2 border-b-primary text-foreground' 
                : 'bg-surface-subtle hover:bg-surface-raised text-muted-foreground hover:text-foreground'
              }
            `}
            onClick={() => onTabChange(tab.path)}
          >
            {/* ファイルアイコン */}
            <span className="text-xs">{getFileIcon()}</span>
            
            {/* ファイル名 */}
            <span className="text-xs font-medium whitespace-nowrap">
              {tab.name}
            </span>
            
            {/* 未保存マーク */}
            {tab.isDirty && (
              <span className="text-primary text-sm leading-none">●</span>
            )}
            
            {/* 閉じるボタン */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                onTabClose(tab.path);
              }}
              className={`
                w-3 h-3 flex items-center justify-center rounded text-xs
                text-muted-foreground hover:text-foreground hover:bg-gray-200 dark:hover:bg-surface-raised
                ${isActive ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}
                transition-opacity
              `}
              title="閉じる (Ctrl+W)"
            >
              ×
            </button>
          </div>
        );
      })}
    </div>
  );
};

export type { OpenTab };
