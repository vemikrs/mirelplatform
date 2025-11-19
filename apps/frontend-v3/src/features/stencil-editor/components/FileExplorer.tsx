/**
 * ファイルエクスプローラーコンポーネント
 * VS Codeライクなファイルツリー表示と操作機能
 */
import React, { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import type { StencilFile } from '../types';

interface FileNode {
  name: string;
  path: string;
  type: 'file' | 'folder';
  children?: FileNode[];
  file?: StencilFile;
}

interface FileExplorerProps {
  files: StencilFile[];
  currentFilePath?: string;
  onFileSelect: (file: StencilFile) => void;
  onFileRename?: (oldPath: string, newPath: string) => void;
  onFileCreate?: (parentPath: string, fileName: string) => void;
  onFileDelete?: (path: string) => void;
  readOnly?: boolean;
  onCollapse?: () => void;
}

export const FileExplorer: React.FC<FileExplorerProps> = ({
  files,
  currentFilePath,
  onFileSelect,
  onFileRename,
  onFileCreate,
  onFileDelete,
  readOnly = false,
  onCollapse,
}) => {
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set(['/']));
  const [renamingPath, setRenamingPath] = useState<string | null>(null);
  const [newName, setNewName] = useState('');
  const [creatingIn, setCreatingIn] = useState<string | null>(null);
  const [newFileName, setNewFileName] = useState('');
  const [focusedPath, setFocusedPath] = useState<string | null>(null);
  const [showShortcuts, setShowShortcuts] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // ファイルリストからツリー構造を構築
  const fileTree = useMemo(() => {
    const root: FileNode = {
      name: 'files',
      path: '/',
      type: 'folder',
      children: [],
    };

    files.forEach((file) => {
      const parts = file.path.split('/').filter(Boolean);
      let current = root;

      parts.forEach((part, index) => {
        const isLastPart = index === parts.length - 1;
        const currentPath = '/' + parts.slice(0, index + 1).join('/');

        if (isLastPart) {
          // ファイルノード
          current.children!.push({
            name: part,
            path: currentPath,
            type: 'file',
            file,
          });
        } else {
          // フォルダノード
          let folder = current.children!.find(
            (child) => child.name === part && child.type === 'folder'
          );
          if (!folder) {
            folder = {
              name: part,
              path: currentPath,
              type: 'folder',
              children: [],
            };
            current.children!.push(folder);
          }
          current = folder;
        }
      });
    });

    // ソート: フォルダ優先、アルファベット順
    const sortNodes = (nodes: FileNode[]) => {
      nodes.sort((a, b) => {
        if (a.type !== b.type) {
          return a.type === 'folder' ? -1 : 1;
        }
        return a.name.localeCompare(b.name);
      });
      nodes.forEach((node) => {
        if (node.children) {
          sortNodes(node.children);
        }
      });
    };
    sortNodes(root.children!);

    return root;
  }, [files]);

  // 全ノードのフラットリストを作成(キーボードナビゲーション用)
  const flattenedNodes = useMemo(() => {
    const nodes: FileNode[] = [];
    const traverse = (node: FileNode) => {
      if (node.path !== '/') {
        nodes.push(node);
      }
      if (node.type === 'folder' && expandedFolders.has(node.path) && node.children) {
        node.children.forEach(traverse);
      }
    };
    if (fileTree.children) {
      fileTree.children.forEach(traverse);
    }
    return nodes;
  }, [fileTree, expandedFolders]);

  const toggleFolder = useCallback((path: string) => {
    setFocusedPath(path);
    setExpandedFolders(prev => {
      const next = new Set(prev);
      if (next.has(path)) {
        next.delete(path);
      } else {
        next.add(path);
      }
      return next;
    });
  }, []);

  const handleFileClick = useCallback((file: StencilFile) => {
    setFocusedPath(file.path);
    onFileSelect(file);
  }, [onFileSelect]);

  // キーボードナビゲーション
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!focusedPath || renamingPath || creatingIn) return;

      const currentIndex = flattenedNodes.findIndex(node => node.path === focusedPath);
      if (currentIndex === -1) return;

      const currentNode = flattenedNodes[currentIndex];

      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          if (currentIndex < flattenedNodes.length - 1) {
            setFocusedPath(flattenedNodes[currentIndex + 1].path);
          }
          break;

        case 'ArrowUp':
          e.preventDefault();
          if (currentIndex > 0) {
            setFocusedPath(flattenedNodes[currentIndex - 1].path);
          }
          break;

        case 'ArrowRight':
          e.preventDefault();
          if (currentNode.type === 'folder' && !expandedFolders.has(currentNode.path)) {
            toggleFolder(currentNode.path);
          }
          break;

        case 'ArrowLeft':
          e.preventDefault();
          if (currentNode.type === 'folder' && expandedFolders.has(currentNode.path)) {
            toggleFolder(currentNode.path);
          }
          break;

        case 'Enter':
          e.preventDefault();
          if (currentNode.type === 'file' && currentNode.file) {
            onFileSelect(currentNode.file);
          } else if (currentNode.type === 'folder') {
            toggleFolder(currentNode.path);
          }
          break;
      }
    };

    if (containerRef.current) {
      containerRef.current.addEventListener('keydown', handleKeyDown);
    }

    return () => {
      const container = containerRef.current;
      if (container) {
        container.removeEventListener('keydown', handleKeyDown);
      }
    };
  }, [focusedPath, flattenedNodes, expandedFolders, renamingPath, creatingIn, onFileSelect, toggleFolder]);

  // 初期フォーカス設定
  useEffect(() => {
    if (!focusedPath && flattenedNodes.length > 0) {
      // Use setTimeout to defer state update to next tick
      const timer = setTimeout(() => {
        setFocusedPath(flattenedNodes[0].path);
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [flattenedNodes, focusedPath]);

  const startRename = (node: FileNode) => {
    if (readOnly) return;
    setRenamingPath(node.path);
    setNewName(node.name);
  };

  const confirmRename = (node: FileNode) => {
    if (!newName.trim() || newName === node.name) {
      setRenamingPath(null);
      return;
    }

    const parentPath = node.path.substring(0, node.path.lastIndexOf('/'));
    const newPath = parentPath + '/' + newName;

    onFileRename?.(node.path, newPath);
    setRenamingPath(null);
    setNewName('');
  };

  const cancelRename = () => {
    setRenamingPath(null);
    setNewName('');
  };

  const startCreateFile = (folderPath: string) => {
    if (readOnly) return;
    setCreatingIn(folderPath);
    setNewFileName('');
  };

  const confirmCreateFile = (folderPath: string) => {
    if (!newFileName.trim()) {
      // 空の場合はキャンセル扱い
      cancelCreateFile();
      return;
    }

    onFileCreate?.(folderPath, newFileName);
    setCreatingIn(null);
    setNewFileName('');
  };

  const cancelCreateFile = () => {
    setCreatingIn(null);
    setNewFileName('');
  };

  const renderNode = (node: FileNode, depth: number = 0): React.ReactNode => {
    const isExpanded = expandedFolders.has(node.path);
    const isSelected = currentFilePath === node.path;
    const isFocused = focusedPath === node.path;
    const isRenaming = renamingPath === node.path;
    const isCreating = creatingIn === node.path;

    return (
      <div key={node.path}>
        <div
          className={`flex items-center gap-1 px-2 py-1 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer group ${
            isFocused ? 'bg-blue-50 dark:bg-blue-900/40 ring-1 ring-blue-300 dark:ring-blue-500' : ''
          } ${
            isSelected ? 'bg-blue-100 dark:bg-blue-800/50' : ''
          }`}
          style={{ paddingLeft: `${depth * 12 + 8}px` }}
        >
          {node.type === 'folder' && (
            <button
              onClick={() => toggleFolder(node.path)}
              className="w-4 h-4 flex items-center justify-center hover:bg-gray-200 dark:hover:bg-gray-600 rounded text-gray-600 dark:text-gray-300"
            >
              <svg
                width="12"
                height="12"
                viewBox="0 0 16 16"
                fill="currentColor"
                className={`transition-transform ${isExpanded ? 'rotate-90' : ''}`}
              >
                <path d="M6 4l4 4-4 4z" />
              </svg>
            </button>
          )}
          {node.type === 'file' && <div className="w-4" />}

          {/* アイコン */}
          <span className={`text-sm ${
            isSelected || isFocused 
              ? 'text-blue-600 dark:text-blue-400' 
              : 'text-gray-600 dark:text-gray-400'
          }`}>
            {node.type === 'folder' ? (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
              </svg>
            ) : (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            )}
          </span>

          {/* ファイル/フォルダ名 */}
          {isRenaming ? (
            <input
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') confirmRename(node);
                if (e.key === 'Escape') cancelRename();
              }}
              onBlur={() => confirmRename(node)}
              autoFocus
              className="flex-1 px-1 py-0 border dark:border-gray-600 rounded text-sm dark:bg-gray-700 dark:text-white"
            />
          ) : (
            <span
              onClick={() => {
                if (node.type === 'file' && node.file) {
                  handleFileClick(node.file);
                } else if (node.type === 'folder') {
                  toggleFolder(node.path);
                }
              }}
              className="flex-1 text-sm whitespace-nowrap dark:text-gray-200"
              title={node.name}
            >
              {node.name}
            </span>
          )}

          {/* アクションボタン */}
          {!readOnly && !isRenaming && (
            <div className="opacity-0 group-hover:opacity-100 flex gap-1">
              {node.type === 'folder' && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    startCreateFile(node.path);
                  }}
                  className="p-0.5 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-600 rounded"
                  title="新規ファイル"
                >
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                  </svg>
                </button>
              )}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  startRename(node);
                }}
                className="p-0.5 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-600 rounded"
                title="名前変更"
              >
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              {node.type === 'file' && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    if (confirm(`${node.name}を削除しますか？`)) {
                      onFileDelete?.(node.path);
                    }
                  }}
                  className="p-0.5 text-red-500 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300 hover:bg-red-50 dark:hover:bg-red-900/30 rounded"
                  title="削除"
                >
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              )}
            </div>
          )}
        </div>

        {/* 子ノード（フォルダの場合） */}
        {node.type === 'folder' && isExpanded && node.children && (
          <div>
            {node.children.map((child) => renderNode(child, depth + 1))}
            {isCreating && (
              <div
                className="flex items-center gap-1 px-2 py-1 bg-blue-50 dark:bg-blue-900/20"
                style={{ paddingLeft: `${(depth + 1) * 12 + 8}px` }}
              >
                <div className="w-4" />
                <svg className="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <input
                  type="text"
                  value={newFileName}
                  onChange={(e) => setNewFileName(e.target.value)}
                  onKeyDown={(e) => {
                    e.stopPropagation();
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      confirmCreateFile(node.path);
                    }
                    if (e.key === 'Escape') {
                      e.preventDefault();
                      cancelCreateFile();
                    }
                  }}
                  onBlur={() => confirmCreateFile(node.path)}
                  placeholder="ファイル名を入力"
                  autoFocus
                  className="flex-1 px-1 py-0 border dark:border-gray-600 rounded text-sm dark:bg-gray-700 dark:text-white"
                />
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

  return (
    <div 
      ref={containerRef}
      className="file-explorer h-full bg-gray-50 dark:bg-gray-800 focus:outline-none"
      style={{
        overflowY: 'auto',
        overflowX: 'auto',
      }}
      tabIndex={0}
      onFocus={() => {
        if (!focusedPath && flattenedNodes.length > 0) {
          setFocusedPath(flattenedNodes[0].path);
        }
      }}
    >
      <div className="p-2 border-b dark:border-gray-700 bg-white dark:bg-gray-800 sticky top-0 z-10">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold dark:text-white">ファイル</span>
            {onCollapse && (
              <button
                onClick={onCollapse}
                className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
                title="エクスプローラーを閉じる"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
              </button>
            )}
          </div>
          <div className="flex gap-1">
            <button
              onClick={() => setShowShortcuts(!showShortcuts)}
              className="p-1 hover:bg-gray-100 rounded transition-colors"
              title="キーボードショートカット"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </button>
            {!readOnly && (
              <button
                onClick={() => startCreateFile('/')}
                className="text-xs px-2 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
              >
                新規ファイル
              </button>
            )}
          </div>
        </div>
        {showShortcuts && (
          <div className="text-xs text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-700 p-2 rounded space-y-1">
            <div><kbd className="px-1 bg-white dark:bg-gray-600 border dark:border-gray-500 rounded">↑↓</kbd> 移動</div>
            <div><kbd className="px-1 bg-white dark:bg-gray-600 border dark:border-gray-500 rounded">←→</kbd> フォルダ開閉</div>
            <div><kbd className="px-1 bg-white dark:bg-gray-600 border dark:border-gray-500 rounded">Enter</kbd> 選択/開閉</div>
          </div>
        )}
      </div>
      <div className="group">
        {fileTree.children?.map((node) => renderNode(node))}
        {creatingIn === '/' && (
          <div 
            className="flex items-center gap-1 px-2 py-1 bg-blue-50 dark:bg-blue-900/20"
            style={{ paddingLeft: '8px' }}
          >
            <div className="w-4" />
            <svg className="w-4 h-4 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <input
              type="text"
              value={newFileName}
              onChange={(e) => setNewFileName(e.target.value)}
              onKeyDown={(e) => {
                e.stopPropagation();
                if (e.key === 'Enter') {
                  e.preventDefault();
                  confirmCreateFile('/');
                }
                if (e.key === 'Escape') {
                  e.preventDefault();
                  cancelCreateFile();
                }
              }}
              onBlur={() => confirmCreateFile('/')}
              placeholder="ファイル名を入力"
              autoFocus
              className="flex-1 px-1 py-0 border rounded text-sm"
            />
          </div>
        )}
      </div>
    </div>
  );
};
