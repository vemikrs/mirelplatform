import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  Button,
  Textarea,
  Input,
  Label,
  useToast,
} from '@mirel/ui';
import { Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api/client';

interface KnowledgeDocumentEditDialogProps {
  fileId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: () => void;
}

export function KnowledgeDocumentEditDialog({ fileId, open, onOpenChange, onSave }: KnowledgeDocumentEditDialogProps) {
  const { toast } = useToast();
  const [content, setContent] = useState('');
  const [description, setDescription] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (open && fileId) {
      loadContent();
    }
  }, [open, fileId]);

  const loadContent = async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<{ content: string; description?: string }>(`/api/mira/knowledge/content/${fileId}`);
      setContent(typeof res.data === 'string' ? res.data : res.data.content);
      setDescription(typeof res.data === 'object' ? res.data.description || '' : '');
    } catch (error) {
      console.error(error);
      toast({
        title: '読み込みエラー',
        description: 'ドキュメント内容の取得に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await apiClient.put(`/api/mira/knowledge/content/${fileId}`, { content, description });
      
      toast({
        title: '保存完了',
        description: 'ドキュメント内容を更新しました。',
      });
      onSave();
    } catch (error) {
      console.error(error);
      toast({
        title: '保存エラー',
        description: '保存に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>ドキュメント編集</DialogTitle>
          <DialogDescription>
            テキスト内容を直接編集します。保存すると再インデックスが行われます。
          </DialogDescription>
        </DialogHeader>
        
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="description">解説文（手動注釈）</Label>
            <Input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="このドキュメントの概要や検索キーワードを入力..."
            />
            <p className="text-xs text-muted-foreground">
              各チャンクに注入され、検索精度が向上します
            </p>
          </div>
        </div>

        <div className="flex-1 min-h-0">
            {isLoading ? (
                <div className="h-full flex justify-center items-center">
                    <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
            ) : (
                <Textarea 
                    value={content} 
                    onChange={(e) => setContent(e.target.value)} 
                    className="h-full font-mono text-sm resize-none"
                />
            )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
            キャンセル
          </Button>
          <Button onClick={handleSave} disabled={isSaving || isLoading}>
            {isSaving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            保存して再インデックス
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

