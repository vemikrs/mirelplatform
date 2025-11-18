/**
 * ファイルエクスプローラーコンポーネント
 * VS Codeライクなファイルツリー表示と操作機能
 */
import React, { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import { toast } from '@mirel/ui';
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
}

export const FileExplorer: React.FC<FileExplorerProps> = ({
  files,
  currentFilePath,
  onFileSelect,
  onFileRename,
  onFileCreate,
  onFileDelete,
  readOnly = false,
}) => {
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set(['/']));
  const [renamingPath, setRenamingPath] = useState<string | null>(null);
  const [newName, setNewName] = useState('');
  const [creatingIn, setCreatingIn] = useState<string | null>(null);
  const [newFileName, setNewFileName] = useState('');
  const [focusedPath, setFocusedPath] = useState<string | null>(null);
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
      toast({
        title: '入力エラー',
        description: 'ファイル名を入力してください',
        variant: 'warning',
      });
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
          className={`flex items-center gap-1 px-2 py-1 hover:bg-gray-100 cursor-pointer group ${
            isFocused ? 'bg-blue-50 ring-1 ring-blue-300' : ''
          } ${
            isSelected ? 'bg-blue-100' : ''
          }`}
          style={{ paddingLeft: `${depth * 12 + 8}px` }}
        >
          {node.type === 'folder' && (
            <button
              onClick={() => toggleFolder(node.path)}
              className="w-4 h-4 flex items-center justify-center hover:bg-gray-200 rounded"
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
          <span className="text-gray-600 text-sm">
            {node.type === 'folder' ? '📁' : '📄'}
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
              className="flex-1 px-1 py-0 border rounded text-sm"
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
              className="flex-1 text-sm truncate"
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
                  className="text-xs text-gray-500 hover:text-gray-700"
                  title="新規ファイル"
                >
                  ➕
                </button>
              )}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  startRename(node);
                }}
                className="text-xs text-gray-500 hover:text-gray-700"
                title="名前変更"
              >
                ✏️
              </button>
              {node.type === 'file' && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    if (confirm(`${node.name}を削除しますか？`)) {
                      onFileDelete?.(node.path);
                    }
                  }}
                  className="text-xs text-red-500 hover:text-red-700"
                  title="削除"
                >
                  🗑️
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
                className="flex items-center gap-1 px-2 py-1 bg-blue-50"
                style={{ paddingLeft: `${(depth + 1) * 12 + 8}px` }}
              >
                <div className="w-4" />
                <span className="text-gray-600 text-sm">📄</span>
                <input
                  type="text"
                  value={newFileName}
                  onChange={(e) => setNewFileName(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') confirmCreateFile(node.path);
                    if (e.key === 'Escape') cancelCreateFile();
                  }}
                  onBlur={() => confirmCreateFile(node.path)}
                  placeholder="ファイル名を入力"
                  autoFocus
                  className="flex-1 px-1 py-0 border rounded text-sm"
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
      className="file-explorer h-full bg-gray-50 overflow-y-auto focus:outline-none"
      tabIndex={0}
      onFocus={() => {
        if (!focusedPath && flattenedNodes.length > 0) {
          setFocusedPath(flattenedNodes[0].path);
        }
      }}
    >
      <div className="p-2 border-b bg-white sticky top-0">
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold">ファイル</span>
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
      <div className="group">
        {fileTree.children?.map((node) => renderNode(node))}
        {creatingIn === '/' && (
          <div className="flex items-center gap-1 px-2 py-1 bg-blue-50">
            <div className="w-4" />
            <span className="text-gray-600 text-sm">📄</span>
            <input
              type="text"
              value={newFileName}
              onChange={(e) => setNewFileName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') confirmCreateFile('/');
                if (e.key === 'Escape') cancelCreateFile();
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
