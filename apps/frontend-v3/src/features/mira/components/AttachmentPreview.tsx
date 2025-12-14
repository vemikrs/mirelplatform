import { useState, useEffect } from 'react';
import { 
  FileText, 
  Image as ImageIcon, 
  FileCode, 
  File, 
  X, 
  Maximize2, 
  Download, 
  Eye 
} from 'lucide-react';
import { Button, Dialog, DialogContent, DialogHeader, DialogTitle, cn } from '@mirel/ui';

export type AttachmentType = 'image' | 'code' | 'text' | 'document' | 'other';

export interface AttachmentItem {
  id: string;
  name: string;
  size: number;
  type: AttachmentType;
  file?: File; // Present when uploading
  fileId?: string; // Present when viewing history
  previewUrl?: string; // Blob URL or Remote URL
}

interface AttachmentPreviewProps {
  item: AttachmentItem;
  onRemove?: (id: string) => void;
  readonly?: boolean;
  className?: string;
  compact?: boolean;
}

/** ファイルサイズをフォーマット */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** ファイルタイプに応じたアイコンマップ */
export const FILE_ICON_MAP = {
  image: ImageIcon,
  code: FileCode,
  text: FileText,
  document: FileText,
  other: File,
} as const;

export function AttachmentPreview({ 
  item, 
  onRemove, 
  readonly = false,
  className,
  compact = false
}: AttachmentPreviewProps) {
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [textContent, setTextContent] = useState<string | null>(null);
  const [isLoadingContent, setIsLoadingContent] = useState(false);
  
  const IconComponent = FILE_ICON_MAP[item.type];

  // テキスト/コードファイルの内容を読み込み
  const loadContent = async () => {
    if (textContent !== null) return;

    if (item.file) {
      // ローカルファイル読み込み
      const reader = new FileReader();
      reader.onload = (e) => {
        setTextContent(e.target?.result as string);
      };
      reader.readAsText(item.file);
    } else if (item.fileId) {
      // リモートファイル読み込み（将来的にAPI実装）
      // 今回はMiraFileController経由で取得することを想定
      try {
        setIsLoadingContent(true);
        const response = await fetch(`/apps/mira/api/files/${item.fileId}`);
        if (response.ok) {
            const text = await response.text();
            setTextContent(text);
        } else {
            setTextContent("プレビューを取得できませんでした。");
        }
      } catch (error) {
        console.error("Failed to fetch file content", error);
        setTextContent("読み込みエラー");
      } finally {
        setIsLoadingContent(false);
      }
    }
  };

  useEffect(() => {
    // ローカルファイルでテキスト/コードの場合は即時読み込み（既存挙動維持）
    if (item.file && (item.type === 'text' || item.type === 'code')) {
      loadContent();
    }
  }, [item.file, item.type]);
  
  // プレビューオープン時の処理
  const handleOpenPreview = () => {
    if (!textContent && (item.type === 'text' || item.type === 'code')) {
        loadContent();
    }
    setIsPreviewOpen(true);
  };
  
  // 画像の場合はサムネイルプレビュー
  if (item.type === 'image' && item.previewUrl) {
    return (
      <>
        <div className={cn("group relative", className)}>
          <button
            onClick={handleOpenPreview}
            className={cn(
              "relative rounded-lg overflow-hidden border bg-muted cursor-pointer hover:ring-2 hover:ring-primary/50 transition-all",
              compact ? "w-10 h-10" : "w-16 h-16"
            )}
          >
            <img 
              src={item.previewUrl} 
              alt={item.name}
              className="w-full h-full object-cover"
            />
            {/* ホバー時のオーバーレイ */}
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Maximize2 className="w-4 h-4 text-white" />
            </div>
          </button>
          {/* 削除ボタン */}
          {!readonly && onRemove && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onRemove(item.id);
              }}
              className="absolute -top-1.5 -right-1.5 p-0.5 rounded-full bg-destructive text-destructive-foreground hover:bg-destructive/90 opacity-0 group-hover:opacity-100 transition-opacity shadow-sm"
            >
              <X className="w-3 h-3" />
            </button>
          )}
          {/* ファイル名ツールチップ風 */}
          {!compact && (
            <p className="mt-0.5 text-[9px] text-muted-foreground truncate max-w-16 text-center">
              {item.name.length > 10 
                ? item.name.substring(0, 7) + '...' 
                : item.name}
            </p>
          )}
        </div>
        
        {/* 画像プレビューダイアログ */}
        <Dialog open={isPreviewOpen} onOpenChange={setIsPreviewOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] p-0 overflow-hidden">
            <DialogHeader className="px-4 py-3 border-b bg-muted/30">
              <DialogTitle className="flex items-center gap-2 text-sm font-medium">
                <IconComponent className="w-4 h-4 text-primary" />
                {item.name}
                <span className="text-muted-foreground font-normal">
                  ({formatFileSize(item.size)})
                </span>
              </DialogTitle>
            </DialogHeader>
            <div className="p-4 flex items-center justify-center bg-muted/20 min-h-[300px] max-h-[70vh] overflow-auto">
              <img 
                src={item.previewUrl} 
                alt={item.name}
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
                  link.href = item.previewUrl!;
                  link.download = item.name;
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
  if (item.type === 'text' || item.type === 'code') {
    return (
      <>
        <div 
          className={cn(
            "group relative flex items-center gap-2 rounded-lg border bg-muted/50 hover:bg-muted transition-colors max-w-[200px] cursor-pointer hover:ring-2 hover:ring-primary/50",
            compact ? "p-1.5 max-w-[150px]" : "px-3 py-2",
            className
          )}
          onClick={handleOpenPreview}
        >
          {/* ファイルアイコン */}
          <div className={cn(
            "rounded-md flex items-center justify-center shrink-0",
            compact ? "w-6 h-6" : "w-8 h-8",
            item.type === 'code' && "bg-blue-500/10 text-blue-500",
            item.type === 'text' && "bg-green-500/10 text-green-500",
          )}>
            <IconComponent className={cn(compact ? "w-3 h-3" : "w-4 h-4")} />
          </div>
          
          {/* ファイル情報 */}
          <div className="flex-1 min-w-0">
            <p className={cn("font-medium truncate", compact ? "text-[10px]" : "text-xs")} title={item.name}>
              {item.name}
            </p>
            <p className="text-[10px] text-muted-foreground flex items-center gap-1">
              {formatFileSize(item.size)}
              <Eye className="w-3 h-3 opacity-60" />
            </p>
          </div>
          
          {/* 削除ボタン */}
          {!readonly && onRemove && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onRemove(item.id);
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
          )}
        </div>
        
        {/* テキスト/コードプレビューダイアログ */}
        <Dialog open={isPreviewOpen} onOpenChange={setIsPreviewOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] p-0 overflow-hidden">
            <DialogHeader className="px-4 py-3 border-b bg-muted/30">
              <DialogTitle className="flex items-center gap-2 text-sm font-medium">
                <IconComponent className={cn(
                  "w-4 h-4",
                  item.type === 'code' ? "text-blue-500" : "text-green-500"
                )} />
                {item.name}
                <span className="text-muted-foreground font-normal">
                  ({formatFileSize(item.size)})
                </span>
              </DialogTitle>
            </DialogHeader>
            <div className="overflow-auto max-h-[60vh] bg-muted/10">
              {isLoadingContent ? (
                  <div className="flex items-center justify-center p-8 text-muted-foreground">
                      読み込み中...
                  </div>
              ) : (
                <pre className={cn(
                    "p-4 text-xs font-mono leading-relaxed whitespace-pre-wrap break-all",
                    item.type === 'code' && "bg-slate-950 text-slate-50"
                )}>
                    <code>{textContent || (item.file ? '' : 'クリックしてプレビュー')}</code>
                </pre>
              )}
            </div>
            <div className="px-4 py-3 border-t bg-muted/30 flex items-center justify-between">
              <span className="text-xs text-muted-foreground">
                {textContent ? `${textContent.split('\n').length} 行` : ''} • Escで閉じる
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                     if (textContent) {
                        navigator.clipboard.writeText(textContent);
                     }
                  }}
                  disabled={!textContent}
                >
                  コピー
                </Button>
                {/* ダウンロード（リモートファイルの場合） */}
                {item.fileId && (
                    <Button
                        variant="outline" 
                        size="sm"
                        onClick={() => {
                            const link = document.createElement('a');
                            link.href = `/apps/mira/api/files/${item.fileId}`;
                            link.download = item.name;
                            link.click();
                        }}
                    >
                        <Download className="w-3.5 h-3.5 mr-1.5" />
                        ダウンロード
                    </Button>
                )}
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </>
    );
  }
  
  // その他のファイルはアイコン + 情報表示（プレビュー不可）
  return (
    <div className={cn(
        "group relative flex items-center gap-2 rounded-lg border bg-muted/50 hover:bg-muted transition-colors max-w-[200px]",
        compact ? "p-1.5 max-w-[150px]" : "px-3 py-2",
        className
    )}>
      {/* ファイルアイコン */}
      <div className={cn(
        "rounded-md flex items-center justify-center shrink-0",
        compact ? "w-6 h-6" : "w-8 h-8",
        item.type === 'document' && "bg-orange-500/10 text-orange-500",
        item.type === 'other' && "bg-gray-500/10 text-gray-500",
      )}>
        <IconComponent className={cn(compact ? "w-3 h-3" : "w-4 h-4")} />
      </div>
      
      {/* ファイル情報 */}
      <div className="flex-1 min-w-0">
        <p className={cn("font-medium truncate", compact ? "text-[10px]" : "text-xs")} title={item.name}>
          {item.name}
        </p>
        <p className="text-[10px] text-muted-foreground">
          {formatFileSize(item.size)}
        </p>
      </div>
      
      {/* 削除ボタン */}
      {!readonly && onRemove && (
        <button
          onClick={() => onRemove(item.id)}
          className={cn(
            "p-1 rounded-full shrink-0",
            "opacity-0 group-hover:opacity-100 transition-opacity",
            "hover:bg-destructive/10 text-destructive"
          )}
          title="削除"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      )}

      {/* ダウンロードボタン（Readonly時のみ表示推奨だが、常に表示でも良いかも。今回はReadonly時、またはクリック動作として実装） */}
      {/* その他のファイルはクリックでダウンロードさせる */}
      {readonly && item.fileId && (
        <a 
            href={`/apps/mira/api/files/${item.fileId}`}
            download={item.name}
            className="absolute inset-0 z-10"
            title="ダウンロード"
        />
      )}
    </div>
  );
}
