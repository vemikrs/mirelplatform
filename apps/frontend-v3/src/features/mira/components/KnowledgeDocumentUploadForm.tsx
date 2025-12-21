import { useState, useRef } from 'react';
import {
  Button,
  useToast,
} from '@mirel/ui';
import { FileText, Loader2, UploadCloud, X } from 'lucide-react';
import { apiClient } from '@/lib/api/client';
import { cn } from '@/lib/utils/cn';

interface KnowledgeDocumentUploadFormProps {
  scope: 'USER' | 'TENANT';
  onUploadSuccess: () => void;
}

export function KnowledgeDocumentUploadForm({ scope, onUploadSuccess }: KnowledgeDocumentUploadFormProps) {
  const { toast } = useToast();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      
      const formData = new FormData();
      formData.append('file', selectedFile);
      
      const uploadRes = await apiClient.post('/commons/upload', formData, {
        headers: {
          'Content-Type': undefined as unknown as string,
        },
      });
      
      const fileId = uploadRes.data.data.uuid; 

      await apiClient.post(`/api/mira/knowledge/index/${fileId}?scope=${scope}`);

      toast({
        title: '登録完了',
        description: 'ドキュメントをナレッジベースに登録しました。',
      });
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';

      onUploadSuccess();
      
    } catch (error) {
      console.error(error);
      toast({
        title: 'エラー',
        description: '登録に失敗しました',
        variant: 'destructive',
      });
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="space-y-4 rounded-xl border bg-card text-card-foreground shadow-sm overflow-hidden h-full flex flex-col">
      <div className="p-6 pb-2">
        <h3 className="font-semibold leading-none tracking-tight flex items-center gap-2 text-lg">
            <UploadCloud className="w-5 h-5 text-primary" />
            新規ドキュメント登録
        </h3>
        <p className="text-sm text-muted-foreground mt-2">
            AIナレッジベースに追加するファイルをアップロードしてください。
            <br />
            {scope === 'USER' ? '※ あなた個人のみが利用可能な「自分専用」スコープです。' : '※ テナント内のメンバー全員が利用可能な「チーム共有」スコープです。'}
        </p>
      </div>

      <div className="flex-1 px-6 pb-6 flex flex-col gap-4">
        {!selectedFile ? (
            <div
                className={cn(
                    "flex-1 border-2 border-dashed rounded-lg flex flex-col items-center justify-center p-8 transition-all cursor-pointer min-h-[200px]",
                    isDragging 
                        ? "border-primary bg-primary/5 scale-[1.02]" 
                        : "border-muted-foreground/25 hover:border-primary/50 hover:bg-muted/50"
                )}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
            >
                <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center mb-4">
                    <UploadCloud className="w-6 h-6 text-primary" />
                </div>
                <p className="text-sm font-medium mb-1">クリックまたはドラッグ＆ドロップ</p>
                <p className="text-xs text-muted-foreground">PDF, TXT, MD, CSV, Excel など</p>
                <input 
                    ref={fileInputRef}
                    id="knowledge-upload-file" 
                    type="file" 
                    onChange={handleFileChange} 
                    disabled={isUploading} 
                    className="hidden" 
                />
            </div>
        ) : (
            <div className="flex-1 flex flex-col justify-center gap-4">
                <div className="relative border rounded-lg p-6 bg-muted/30 flex flex-col items-center gap-3 animate-in fade-in zoom-in-95 duration-200">
                    <button 
                        onClick={() => setSelectedFile(null)}
                        className="absolute top-2 right-2 p-1 rounded-full hover:bg-muted text-muted-foreground transition-colors"
                        disabled={isUploading}
                    >
                        <X className="w-4 h-4" />
                    </button>
                    
                    <div className="w-16 h-16 rounded-lg bg-background border shadow-sm flex items-center justify-center">
                        <FileText className="w-8 h-8 text-primary" />
                    </div>
                    <div className="text-center">
                        <p className="font-medium text-sm text-foreground break-all line-clamp-2 max-w-[200px]">
                            {selectedFile.name}
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">
                            {(selectedFile.size / 1024).toFixed(1)} KB
                        </p>
                    </div>
                </div>

                <Button onClick={handleUpload} disabled={isUploading} className="w-full h-11 text-base shadow-lg shadow-primary/20 transition-all hover:scale-[1.02]">
                    {isUploading ? (
                        <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            アップロード中...
                        </>
                    ) : (
                        scope === 'USER' ? '自分専用として登録' : 'チーム共有として登録'
                    )}
                </Button>
            </div>
        )}
      </div>
    </div>
  );
}
