/**
 * Mira Chat Input Component
 * 
 * メッセージ入力フォーム + @メンション風モード選択 + 入力履歴 + ファイル添付
 */
import { useState, useRef, useEffect, type KeyboardEvent, type ChangeEvent, type DragEvent } from 'react';
import { Button, cn, Dialog, DialogContent, DialogHeader, DialogTitle } from '@mirel/ui';
import { 
  Send, 
  Loader2, 
  ChevronDown, 
  HelpCircle, 
  AlertTriangle, 
  Paintbrush2, 
  Workflow, 
  MessageSquare,
  Paperclip,
  X,
  FileText,
  Image,
  FileCode,
  File,
  Upload,
  Eye,
  Download,
  Maximize2,
  Settings, // Add Settings icon
} from 'lucide-react';
import { ContextSwitcherModal } from './ContextSwitcherModal';
import { type MessageConfig } from '@/lib/api/mira';

type MiraMode = 'GENERAL_CHAT' | 'CONTEXT_HELP' | 'ERROR_ANALYZE' | 'STUDIO_AGENT' | 'WORKFLOW_AGENT';

/** モード定義 */
const MODES: { mode: MiraMode; label: string; shortLabel: string; icon: typeof MessageSquare; color: string }[] = [
  { mode: 'GENERAL_CHAT', label: 'General', shortLabel: 'General', icon: MessageSquare, color: 'text-foreground' },
  { mode: 'CONTEXT_HELP', label: 'Help - 機能の説明', shortLabel: 'Help', icon: HelpCircle, color: 'text-blue-500' },
  { mode: 'ERROR_ANALYZE', label: 'Error - エラー分析', shortLabel: 'Error', icon: AlertTriangle, color: 'text-red-500' },
  { mode: 'STUDIO_AGENT', label: 'Studio - モデル設計', shortLabel: 'Studio', icon: Paintbrush2, color: 'text-purple-500' },
  { mode: 'WORKFLOW_AGENT', label: 'Workflow - フロー設計', shortLabel: 'Workflow', icon: Workflow, color: 'text-green-500' },
];

// 入力履歴の最大保持数
const MAX_HISTORY = 50;

// 添付ファイルの最大サイズ（10MB）
const MAX_FILE_SIZE = 10 * 1024 * 1024;

// 許可するファイルタイプ
const ALLOWED_FILE_TYPES = [
  'image/*',
  'text/*',
  'application/json',
  'application/javascript',
  'application/typescript',
  'application/pdf',
  '.md', '.tsx', '.ts', '.jsx', '.js', '.py', '.java', '.xml', '.yaml', '.yml', '.sql', '.sh', '.css', '.scss', '.html',
];

/** 添付ファイル型 */
export interface AttachedFile {
  id: string;
  file: File;
  preview?: string;
  type: 'image' | 'code' | 'text' | 'document' | 'other';
}

interface MiraChatInputProps {
  onSend: (message: string, mode?: MiraMode, config?: MessageConfig) => void;
  isLoading?: boolean;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
  showShortcuts?: boolean;
  initialMode?: MiraMode;
  autoFocus?: boolean;
  /** コンパクト表示（ポップアップウィンドウ用） */
  compact?: boolean;
  /** 編集中のメッセージID */
  editingMessageId?: string;
  /** 編集中のメッセージ内容 */
  editingMessageContent?: string;
  /** 編集キャンセルコールバック */
  onCancelEdit?: () => void;
}

export function MiraChatInput({
  onSend,
  isLoading = false,
  disabled = false,
  placeholder = 'メッセージを入力...',
  className,
  initialMode = 'GENERAL_CHAT',
  autoFocus = false,
  compact = false,
  editingMessageId,
  editingMessageContent,
  onCancelEdit,
}: MiraChatInputProps) {
  const [message, setMessage] = useState('');
  const [selectedMode, setSelectedMode] = useState<MiraMode>(initialMode);
  const [showModeMenu, setShowModeMenu] = useState(false);
  
  // 入力履歴管理
  const [inputHistory, setInputHistory] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('mira-input-history');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [contextModalOpen, setContextModalOpen] = useState(false);
  const [messageConfig, setMessageConfig] = useState<MessageConfig>({});

  const [historyIndex, setHistoryIndex] = useState(-1);
  const [tempMessage, setTempMessage] = useState(''); // 履歴ナビ前のメッセージを保持
  
  // 添付ファイル管理
  const [attachedFiles, setAttachedFiles] = useState<AttachedFile[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  // 自動リサイズ
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [message]);
  
  // メニュー外クリックで閉じる
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowModeMenu(false);
      }
    };
    if (showModeMenu) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showModeMenu]);
  
  // 入力履歴をlocalStorageに保存
  useEffect(() => {
    localStorage.setItem('mira-input-history', JSON.stringify(inputHistory));
  }, [inputHistory]);
  
  // autoFocusまたはisLoadingがfalseになった時にフォーカス
  useEffect(() => {
    if (autoFocus && !isLoading && !disabled) {
      // 少し遅延を入れてDOM更新後にフォーカス
      const timer = setTimeout(() => {
        textareaRef.current?.focus();
      }, 50);
      return () => clearTimeout(timer);
    }
  }, [autoFocus, isLoading, disabled]);
  
  // 編集モード時にメッセージ内容をセット
  useEffect(() => {
    if (editingMessageId && editingMessageContent !== undefined) {
      setMessage(editingMessageContent);
      // 編集モード時は履歴をリセット
      setHistoryIndex(-1);
      setTempMessage('');
      // フォーカス
      setTimeout(() => {
        textareaRef.current?.focus();
      }, 50);
    }
  }, [editingMessageId, editingMessageContent]);
  
  // ファイルタイプを判定
  const getFileType = (file: File): AttachedFile['type'] => {
    const mime = file.type;
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    
    if (mime.startsWith('image/')) return 'image';
    if (['ts', 'tsx', 'js', 'jsx', 'py', 'java', 'json', 'xml', 'yaml', 'yml', 'sql', 'sh', 'css', 'scss', 'html'].includes(ext)) return 'code';
    if (mime.startsWith('text/') || ['md', 'txt', 'log'].includes(ext)) return 'text';
    if (mime === 'application/pdf') return 'document';
    return 'other';
  };
  
  // ファイル追加処理
  const handleAddFiles = (files: FileList | File[]) => {
    const fileArray = Array.from(files);
    
    const newFiles: AttachedFile[] = fileArray
      .filter(file => file.size <= MAX_FILE_SIZE)
      .map(file => ({
        id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        file,
        type: getFileType(file),
        preview: file.type.startsWith('image/') ? URL.createObjectURL(file) : undefined,
      }));
    
    setAttachedFiles(prev => [...prev, ...newFiles]);
  };
  
  // ファイル削除処理
  const handleRemoveFile = (id: string) => {
    setAttachedFiles(prev => {
      const file = prev.find(f => f.id === id);
      if (file?.preview) {
        URL.revokeObjectURL(file.preview);
      }
      return prev.filter(f => f.id !== id);
    });
  };
  
  // ドラッグ&ドロップ処理
  const handleDragOver = (e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };
  
  const handleDragLeave = (e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };
  
  const handleDrop = (e: DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    
    if (e.dataTransfer.files.length > 0) {
      handleAddFiles(e.dataTransfer.files);
    }
  };
  
  // ファイル選択処理
  const handleFileInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleAddFiles(e.target.files);
      e.target.value = ''; // リセット
    }
  };
  
  const handleSend = () => {
    const trimmed = message.trim();
    if ((trimmed || attachedFiles.length > 0) && !isLoading && !disabled) {
      // 履歴に追加（重複は除く）
      if (trimmed) {
        setInputHistory((prev) => {
          const filtered = prev.filter((h) => h !== trimmed);
          return [trimmed, ...filtered].slice(0, MAX_HISTORY);
        });
      }
      
      
      // TODO: 添付ファイルも送信処理に含める
      onSend(trimmed, selectedMode, messageConfig);
      
      setMessage('');
      setMessageConfig({}); // Reset config
      setHistoryIndex(-1);
      setTempMessage('');
      // 添付ファイルをクリア
      attachedFiles.forEach(f => {
        if (f.preview) URL.revokeObjectURL(f.preview);
      });
      setAttachedFiles([]);
    }
  };
  
  const handleModeSelect = (mode: MiraMode) => {
    setSelectedMode(mode);
    setShowModeMenu(false);
    textareaRef.current?.focus();
  };
  
  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    // Ctrl/Cmd + Shift + M でコンテキストスイッチャー
    if (e.key === 'M' && (e.ctrlKey || e.metaKey) && e.shiftKey) {
      e.preventDefault();
      setContextModalOpen(true);
      return;
    }
    
    // Ctrl/Cmd + Enter で送信
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      handleSend();
      return;
    }
    // Enter のみは送信（Shift + Enter は改行）
    if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
      handleSend();
      return;
    }
    // @ でモードメニューを開く
    if (e.key === '@' && message === '') {
      e.preventDefault();
      setShowModeMenu(true);
      return;
    }
    // Escape でメニューを閉じる
    if (e.key === 'Escape' && showModeMenu) {
      setShowModeMenu(false);
      return;
    }
    // Escape で編集モードをキャンセル
    if (e.key === 'Escape' && editingMessageId && onCancelEdit) {
      e.preventDefault();
      onCancelEdit();
      return;
    }
    
    // 上下キーで入力履歴をナビゲート
    if (e.key === 'ArrowUp' && inputHistory.length > 0) {
      e.preventDefault();
      if (historyIndex === -1) {
        // 最初の履歴アクセス時、現在の入力を保存
        setTempMessage(message);
      }
      const newIndex = Math.min(historyIndex + 1, inputHistory.length - 1);
      setHistoryIndex(newIndex);
      const historyValue = inputHistory[newIndex];
      if (historyValue) setMessage(historyValue);
      return;
    }
    
    if (e.key === 'ArrowDown' && historyIndex >= 0) {
      e.preventDefault();
      const newIndex = historyIndex - 1;
      setHistoryIndex(newIndex);
      if (newIndex < 0) {
        // 履歴を抜けたら保存していた入力を復元
        setMessage(tempMessage);
      } else {
        const historyValue = inputHistory[newIndex];
        if (historyValue) setMessage(historyValue);
      }
      return;
    }
  };
  
  // モードボタンクリック時のトグル動作
  const handleModeButtonClick = () => {
    setShowModeMenu((prev) => !prev);
  };
  
  const currentModeConfig = MODES.find(m => m.mode === selectedMode) ?? MODES[0]!;
  const CurrentIcon = currentModeConfig.icon;
  
  return (
    <div 
      className={cn('', className)}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* 隠しファイル入力 */}
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept={ALLOWED_FILE_TYPES.join(',')}
        onChange={handleFileInputChange}
        className="hidden"
      />
      
      {/* ドラッグ&ドロップオーバーレイ */}
      {isDragging && (
        <div className="absolute inset-0 z-20 flex items-center justify-center bg-primary/5 border-2 border-dashed border-primary rounded-lg">
          <div className="flex flex-col items-center gap-2 text-primary">
            <Upload className="w-8 h-8 animate-bounce" />
            <p className="text-sm font-medium">ファイルをドロップ</p>
          </div>
        </div>
      )}
      
      {/* 編集中バナー */}
      {editingMessageId && onCancelEdit && (
        <div className="px-3 py-2 bg-amber-500/10 border-b flex items-center justify-between">
          <span className="text-xs text-amber-700 dark:text-amber-400 font-medium">
            メッセージを編集中
          </span>
          <Button 
            size="sm" 
            variant="ghost" 
            onClick={onCancelEdit}
            className="h-6 text-xs"
          >
            キャンセル
          </Button>
        </div>
      )}
      
      {/* メッセージ入力エリア */}
      <div className="relative px-3 py-2">
        {/* モード選択メニュー */}
        {showModeMenu && (
          <div 
            ref={menuRef}
            className="absolute bottom-full left-3 mb-2 w-56 py-1 bg-popover border rounded-lg shadow-lg z-10"
          >
            <p className="px-3 py-1.5 text-xs font-medium text-muted-foreground">
              モードを選択
            </p>
            {MODES.map(({ mode, label, icon: Icon, color }) => (
              <button
                key={mode}
                onClick={() => handleModeSelect(mode)}
                className={cn(
                  "w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted transition-colors",
                  selectedMode === mode && "bg-muted"
                )}
              >
                <Icon className={cn("w-4 h-4", color)} />
                <span>{label}</span>
              </button>
            ))}
          </div>
        )}
        
        {/* 添付ファイルプレビュー */}
        {attachedFiles.length > 0 && (
          <div className="mb-2 flex flex-wrap gap-2">
            {attachedFiles.map((file) => (
              <AttachmentPreview
                key={file.id}
                file={file}
                onRemove={() => handleRemoveFile(file.id)}
              />
            ))}
          </div>
        )}
        
        <div className="flex items-end gap-2">
          {/* 添付ボタン */}
          <button
            onClick={() => fileInputRef.current?.click()}
            className={cn(
              "p-1.5 rounded-md border",
              "hover:bg-muted transition-colors shrink-0",
              "text-muted-foreground hover:text-foreground"
            )}
            title="ファイルを添付"
            disabled={isLoading || disabled}
          >
            <Paperclip className="w-4 h-4" />
          </button>

          {/* コンテキスト設定ボタン */}
          <button
            onClick={() => setContextModalOpen(true)}
            className={cn(
              "p-1.5 rounded-md border",
              "hover:bg-muted transition-colors shrink-0",
              "text-muted-foreground hover:text-foreground",
              // 設定がある場合は色を変える
              Object.keys(messageConfig).length > 0 && "bg-purple-100 dark:bg-purple-900 border-purple-200 dark:border-purple-800 text-purple-600 dark:text-purple-400"
            )}
            title="コンテキスト設定 (Ctrl+Shift+M)"
            disabled={isLoading || disabled}
          >
            <Settings className="w-4 h-4" />
          </button>
          
          {/* モード選択ボタン */}
          <button
            onClick={handleModeButtonClick}
            className={cn(
              "flex items-center gap-1 px-2 py-1.5 text-xs rounded-md border",
              "hover:bg-muted transition-colors shrink-0",
              currentModeConfig.color
            )}
            title="モードを変更 (@)"
          >
            <CurrentIcon className="w-3.5 h-3.5" />
            <span>{currentModeConfig.shortLabel}</span>
            <ChevronDown className={cn("w-3 h-3 opacity-50 transition-transform", showModeMenu && "rotate-180")} />
          </button>
          
          {/* テキストエリア */}
          <textarea
            ref={textareaRef}
            value={message}
            onChange={(e) => {
              setMessage(e.target.value);
              // 手動入力時は履歴インデックスをリセット
              if (historyIndex >= 0) {
                setHistoryIndex(-1);
                setTempMessage('');
              }
            }}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            disabled={isLoading || disabled}
            rows={1}
            className={cn(
              'flex-1 resize-none rounded-md border border-input bg-transparent px-3 py-2',
              'text-sm placeholder:text-muted-foreground',
              'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
              'disabled:cursor-not-allowed disabled:opacity-50',
              'overflow-hidden'
            )}
          />
          
          {/* 送信ボタン */}
          <Button
            onClick={handleSend}
            disabled={(!message.trim() && attachedFiles.length === 0) || isLoading || disabled}
            size="icon"
            className="shrink-0"
          >
            {isLoading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </Button>
        </div>
        
        {/* ヒント表示（コンパクト時は非表示） */}
        {!compact && (
          <p className="text-[10px] text-muted-foreground mt-1.5 text-center">
            Enter で送信 • Shift+Enter で改行 • @ でモード選択 • ↑↓ で履歴
          </p>
        )}
      </div>

      <ContextSwitcherModal 
        open={contextModalOpen} 
        onOpenChange={setContextModalOpen}
        config={messageConfig}
        onConfigChange={setMessageConfig}
        messageContent={message}
      />
    </div>
  );
}

/** ファイルタイプに応じたアイコンマップ */
const FILE_ICON_MAP = {
  image: Image,
  code: FileCode,
  text: FileText,
  document: FileText,
  other: File,
} as const;

/** ファイルサイズをフォーマット */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** 添付ファイルプレビューコンポーネント */
function AttachmentPreview({ 
  file, 
  onRemove 
}: { 
  file: AttachedFile; 
  onRemove: () => void;
}) {
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [textContent, setTextContent] = useState<string | null>(null);
  const IconComponent = FILE_ICON_MAP[file.type];
  
  // テキスト/コードファイルの内容を読み込み
  useEffect(() => {
    if (file.type === 'text' || file.type === 'code') {
      const reader = new FileReader();
      reader.onload = (e) => {
        setTextContent(e.target?.result as string);
      };
      reader.readAsText(file.file);
    }
  }, [file]);
  
  // 画像の場合はサムネイルプレビュー
  if (file.type === 'image' && file.preview) {
    return (
      <>
        <div className="group relative">
          <button
            onClick={() => setIsPreviewOpen(true)}
            className="relative w-16 h-16 rounded-lg overflow-hidden border bg-muted cursor-pointer hover:ring-2 hover:ring-primary/50 transition-all"
          >
            <img 
              src={file.preview} 
              alt={file.file.name}
              className="w-full h-full object-cover"
            />
            {/* ホバー時のオーバーレイ */}
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Maximize2 className="w-4 h-4 text-white" />
            </div>
          </button>
          {/* 削除ボタン */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              onRemove();
            }}
            className="absolute -top-1.5 -right-1.5 p-0.5 rounded-full bg-destructive text-destructive-foreground hover:bg-destructive/90 opacity-0 group-hover:opacity-100 transition-opacity shadow-sm"
          >
            <X className="w-3 h-3" />
          </button>
          {/* ファイル名ツールチップ風 */}
          <p className="mt-0.5 text-[9px] text-muted-foreground truncate max-w-16 text-center">
            {file.file.name.length > 10 
              ? file.file.name.substring(0, 7) + '...' 
              : file.file.name}
          </p>
        </div>
        
        {/* 画像プレビューダイアログ */}
        <Dialog open={isPreviewOpen} onOpenChange={setIsPreviewOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] p-0 overflow-hidden">
            <DialogHeader className="px-4 py-3 border-b bg-muted/30">
              <DialogTitle className="flex items-center gap-2 text-sm font-medium">
                <Image className="w-4 h-4 text-primary" />
                {file.file.name}
                <span className="text-muted-foreground font-normal">
                  ({formatFileSize(file.file.size)})
                </span>
              </DialogTitle>
            </DialogHeader>
            <div className="p-4 flex items-center justify-center bg-muted/20 min-h-[300px] max-h-[70vh] overflow-auto">
              <img 
                src={file.preview} 
                alt={file.file.name}
                className="max-w-full max-h-full object-contain rounded-lg shadow-lg"
              />
            </div>
            <div className="px-4 py-3 border-t bg-muted/30 flex items-center justify-between">
              <span className="text-xs text-muted-foreground">
                クリックで拡大 • Escで閉じる
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  const link = document.createElement('a');
                  link.href = file.preview!;
                  link.download = file.file.name;
                  link.click();
                }}
              >
                <Download className="w-3.5 h-3.5 mr-1.5" />
                ダウンロード
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </>
    );
  }
  
  // テキスト/コードファイルの場合
  if ((file.type === 'text' || file.type === 'code') && textContent !== null) {
    return (
      <>
        <div className="group relative flex items-center gap-2 px-3 py-2 rounded-lg border bg-muted/50 hover:bg-muted transition-colors max-w-[200px] cursor-pointer hover:ring-2 hover:ring-primary/50"
          onClick={() => setIsPreviewOpen(true)}
        >
          {/* ファイルアイコン */}
          <div className={cn(
            "w-8 h-8 rounded-md flex items-center justify-center shrink-0",
            file.type === 'code' && "bg-blue-500/10 text-blue-500",
            file.type === 'text' && "bg-green-500/10 text-green-500",
          )}>
            <IconComponent className="w-4 h-4" />
          </div>
          
          {/* ファイル情報 */}
          <div className="flex-1 min-w-0">
            <p className="text-xs font-medium truncate" title={file.file.name}>
              {file.file.name}
            </p>
            <p className="text-[10px] text-muted-foreground flex items-center gap-1">
              {formatFileSize(file.file.size)}
              <Eye className="w-3 h-3 opacity-60" />
            </p>
          </div>
          
          {/* 削除ボタン */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              onRemove();
            }}
            className={cn(
              "p-1 rounded-full shrink-0",
              "opacity-0 group-hover:opacity-100 transition-opacity",
              "hover:bg-destructive/10 text-destructive"
            )}
            title="削除"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
        
        {/* テキスト/コードプレビューダイアログ */}
        <Dialog open={isPreviewOpen} onOpenChange={setIsPreviewOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] p-0 overflow-hidden">
            <DialogHeader className="px-4 py-3 border-b bg-muted/30">
              <DialogTitle className="flex items-center gap-2 text-sm font-medium">
                <IconComponent className={cn(
                  "w-4 h-4",
                  file.type === 'code' ? "text-blue-500" : "text-green-500"
                )} />
                {file.file.name}
                <span className="text-muted-foreground font-normal">
                  ({formatFileSize(file.file.size)})
                </span>
              </DialogTitle>
            </DialogHeader>
            <div className="overflow-auto max-h-[60vh] bg-muted/10">
              <pre className={cn(
                "p-4 text-xs font-mono leading-relaxed whitespace-pre-wrap break-all",
                file.type === 'code' && "bg-slate-950 text-slate-50"
              )}>
                <code>{textContent}</code>
              </pre>
            </div>
            <div className="px-4 py-3 border-t bg-muted/30 flex items-center justify-between">
              <span className="text-xs text-muted-foreground">
                {textContent.split('\n').length} 行 • Escで閉じる
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    navigator.clipboard.writeText(textContent);
                  }}
                >
                  コピー
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </>
    );
  }
  
  // その他のファイルはアイコン + 情報表示（プレビュー不可）
  return (
    <div className="group relative flex items-center gap-2 px-3 py-2 rounded-lg border bg-muted/50 hover:bg-muted transition-colors max-w-[200px]">
      {/* ファイルアイコン */}
      <div className={cn(
        "w-8 h-8 rounded-md flex items-center justify-center shrink-0",
        file.type === 'document' && "bg-orange-500/10 text-orange-500",
        file.type === 'other' && "bg-gray-500/10 text-gray-500",
      )}>
        <IconComponent className="w-4 h-4" />
      </div>
      
      {/* ファイル情報 */}
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium truncate" title={file.file.name}>
          {file.file.name}
        </p>
        <p className="text-[10px] text-muted-foreground">
          {formatFileSize(file.file.size)}
        </p>
      </div>
      
      {/* 削除ボタン */}
      <button
        onClick={onRemove}
        className={cn(
          "p-1 rounded-full shrink-0",
          "opacity-0 group-hover:opacity-100 transition-opacity",
          "hover:bg-destructive/10 text-destructive"
        )}
        title="削除"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  );
}
