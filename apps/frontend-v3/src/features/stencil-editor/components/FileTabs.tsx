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

const getFileIcon = (type: FileType): string => {
  switch (type) {
    case 'template':
      return '📝';
    case 'yaml':
      return '📄';
    default:
      return '📄';
  }
};

export const FileTabs: React.FC<FileTabsProps> = ({
  tabs,
  activeTab,
  onTabChange,
  onTabClose,
}) => {
  if (tabs.length === 0) {
    return (
      <div className="h-7 border-b flex items-center px-2 bg-gray-50 text-xs text-gray-500">
        ← ファイルを選択してください
      </div>
    );
  }

  return (
    <div 
      className="flex border-b bg-gray-50"
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
              group flex items-center gap-1 px-2 py-1 min-w-fit cursor-pointer border-r
              transition-colors select-none
              ${isActive 
                ? 'bg-white border-b-2 border-b-blue-500 text-gray-900' 
                : 'bg-gray-50 hover:bg-gray-100 text-gray-600 hover:text-gray-900'
              }
            `}
            onClick={() => onTabChange(tab.path)}
          >
            {/* ファイルアイコン */}
            <span className="text-xs">{getFileIcon(tab.type)}</span>
            
            {/* ファイル名 */}
            <span className="text-xs font-medium whitespace-nowrap">
              {tab.name}
            </span>
            
            {/* 未保存マーク */}
            {tab.isDirty && (
              <span className="text-blue-500 text-sm leading-none">●</span>
            )}
            
            {/* 閉じるボタン */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                onTabClose(tab.path);
              }}
              className={`
                w-3 h-3 flex items-center justify-center rounded text-xs
                text-gray-400 hover:text-gray-700 hover:bg-gray-200
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
