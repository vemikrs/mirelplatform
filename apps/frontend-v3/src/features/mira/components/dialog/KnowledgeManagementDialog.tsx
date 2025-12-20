import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Button,
  Input,
  Label,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  useToast,
} from '@mirel/ui';
import { FileText, Loader2, Database } from 'lucide-react';
import { apiClient } from '@/lib/api/client';

interface KnowledgeManagementDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function KnowledgeManagementDialog({
  open,
  onOpenChange,
}: KnowledgeManagementDialogProps) {
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState('upload');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [scope, setScope] = useState<'USER' | 'TENANT'>('USER'); // System scope usually for admin only

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      
      // 1. Upload File to FileManagement (Existing API)
      const formData = new FormData();
      formData.append('file', selectedFile);
      
      // Override default Content-Type to let axios auto-detect FormData and set correct boundary
      const uploadRes = await apiClient.post('/files/register', formData, {
        headers: {
          'Content-Type': undefined as unknown as string,
        },
      });
      
      const fileId = uploadRes.data.fileId; // Adjust based on actual response

      // 2. Index File to Knowledge Base
      await apiClient.post(`/apps/mira/knowledge/index/${fileId}?scope=${scope}`);

      toast({
        title: '登録完了',
        description: 'ドキュメントをナレッジベースに登録しました。',
      });
      setSelectedFile(null);
      // TODO: Refresh list if implemented
      
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
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Database className="w-5 h-5" />
            ナレッジベース管理
          </DialogTitle>
          <DialogDescription>
            AIが参照するためのドキュメントを登録・管理します。
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="upload" value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="upload">新規登録</TabsTrigger>
            <TabsTrigger value="list">登録済み一覧</TabsTrigger>
          </TabsList>
          
          <TabsContent value="upload" className="space-y-4 py-4">
            <div className="grid w-full max-w-sm items-center gap-1.5">
              <Label htmlFor="scope">公開範囲</Label>
              <div className="flex gap-4">
                  <label className="flex items-center gap-2 text-sm">
                      <input 
                        type="radio" 
                        name="scope" 
                        value="USER" 
                        checked={scope === 'USER'} 
                        onChange={() => setScope('USER')}
                      />
                      自分のみ (Private)
                  </label>
                  <label className="flex items-center gap-2 text-sm">
                      <input 
                        type="radio" 
                        name="scope" 
                        value="TENANT" 
                        checked={scope === 'TENANT'} 
                        onChange={() => setScope('TENANT')}
                      />
                      テナント共有 (Team)
                  </label>
              </div>
            </div>

            <div className="grid w-full max-w-sm items-center gap-1.5">
              <Label htmlFor="file">ドキュメントファイル (PDF, txt, md, etc)</Label>
              <Input id="file" type="file" onChange={handleFileChange} disabled={isUploading} />
            </div>

            {selectedFile && (
                <div className="bg-muted p-2 rounded text-sm flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    {selectedFile.name}
                    <span className="text-xs text-muted-foreground ml-auto">
                        {(selectedFile.size / 1024).toFixed(1)} KB
                    </span>
                </div>
            )}
            
            <div className="pt-4 flex justify-end">
                <Button onClick={handleUpload} disabled={!selectedFile || isUploading}>
                    {isUploading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    登録実行
                </Button>
            </div>
          </TabsContent>
          
          <TabsContent value="list" className="py-4">
             <div className="text-center text-muted-foreground py-8">
                一覧表示機能は実装中です。
             </div>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
