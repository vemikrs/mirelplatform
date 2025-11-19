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
      <div className="h-7 border-b dark:border-gray-700 flex items-center px-2 bg-gray-50 dark:bg-gray-800 text-xs text-gray-500 dark:text-gray-400">
        ← ファイルを選択してください
      </div>
    );
  }

  return (
    <div 
      className="flex border-b dark:border-gray-700 bg-gray-50 dark:bg-gray-800"
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
              group flex items-center gap-1 px-2 py-1 min-w-fit cursor-pointer border-r dark:border-gray-700
              transition-colors select-none
              ${isActive 
                ? 'bg-white dark:bg-gray-900 border-b-2 border-b-blue-500 dark:border-b-blue-400 text-gray-900 dark:text-white' 
                : 'bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
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
              <span className="text-blue-500 dark:text-blue-400 text-sm leading-none">●</span>
            )}
            
            {/* 閉じるボタン */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                onTabClose(tab.path);
              }}
              className={`
                w-3 h-3 flex items-center justify-center rounded text-xs
                text-gray-400 dark:text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600
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
