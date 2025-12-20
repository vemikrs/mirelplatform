import { useState } from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
  Button,
  Input,
  Label,
  useToast,
  Badge,
} from '@mirel/ui';
import { FileText, Loader2, ShieldAlert, Upload } from 'lucide-react';
import { KnowledgeDocumentList } from '@/features/mira/components/KnowledgeDocumentList';

export default function SystemKnowledgeAdminPage() {
  const { toast } = useToast();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setIsUploading(true);

      // 1. Upload File (simulated API call for now, same as user dialog)
      const formData = new FormData();
      formData.append('file', selectedFile);
      // Explicitly unsetting Content-Type is handled by browser for FormData, 
      // but some frontend setups need care. Here standard fetch handles it.
      
      const uploadRes = await fetch('/api/files/register', {
        method: 'POST',
        headers: {
             'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: formData
      });
      
      if (!uploadRes.ok) throw new Error('File upload failed');
      const uploadData = await uploadRes.json();
      const fileId = uploadData.fileId;

      // 2. Index File to SYSTEM Scope
      // Note: Backend should enforce ADMIN role for scope=SYSTEM
      const indexRes = await fetch(`/api/mira/knowledge/index/${fileId}?scope=SYSTEM`, {
        method: 'POST',
        headers: {
             'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
      });

      if (!indexRes.ok) throw new Error('Indexing failed');

      toast({
        title: 'システムナレッジ登録完了',
        description: '全ユーザーが参照可能なドキュメントとして登録されました。',
      });
      setSelectedFile(null);
      
      // Refresh list
      setRefreshTrigger(prev => prev + 1);
      // Reset file input if possible (ref would be better)
      const fileInput = document.getElementById('file') as HTMLInputElement;
      if (fileInput) fileInput.value = '';

    } catch (error) {
      console.error(error);
      toast({
        title: 'エラー',
        description: '登録に失敗しました。管理者権限を確認してください。',
        variant: 'destructive',
      });
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="container mx-auto py-8 space-y-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">システムナレッジ管理</h1>
        <p className="text-muted-foreground mt-2">
          Mira AI全体の知識ベースを管理します。ここで登録されたドキュメントは、
          <span className="font-bold text-red-500 mx-1">全テナント・全ユーザー</span>
          から参照可能になります。
          機密情報の取り扱いに十分注意してください。
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Upload Column */}
        <div className="lg:col-span-1">
            <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2">
                <ShieldAlert className="w-5 h-5 text-red-500" />
                新規登録 (System Scope)
                </CardTitle>
                <CardDescription>
                システム全体で共有される読み取り専用の知識データをアップロードします。
                </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="p-4 border-2 border-dashed rounded-lg bg-muted/50 text-center">
                    <div className="grid w-full items-center gap-4">
                        <Label htmlFor="file" className="cursor-pointer">
                            <div className="flex flex-col items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
                                <Upload className="h-8 w-8" />
                                <span className="text-sm font-medium">クリックしてファイルを選択</span>
                            </div>
                        </Label>
                        <Input id="file" type="file" onChange={handleFileChange} disabled={isUploading} className="hidden" />
                    </div>
                </div>

                {selectedFile && (
                    <div className="bg-primary/10 p-3 rounded-md text-sm flex items-center gap-2 border border-primary/20">
                        <FileText className="w-4 h-4 text-primary" />
                        <span className="font-medium truncate flex-1">{selectedFile.name}</span>
                        <Badge variant="destructive" className="ml-auto text-xs shrink-0">GLOBAL</Badge>
                    </div>
                )}

                <Button 
                    onClick={handleUpload} 
                    disabled={!selectedFile || isUploading}
                    className="w-full"
                    variant="destructive" // Alerting color for system-wide action
                >
                    {isUploading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    システム全体に公開して登録
                </Button>
            </CardContent>
            </Card>
        </div>

        {/* List Column */}
        <div className="lg:col-span-2">
            <Card>
                <CardHeader>
                    <CardTitle>登録済みドキュメント</CardTitle>
                    <CardDescription>
                    現在システムスコープで登録されているドキュメント一覧
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <KnowledgeDocumentList scope="SYSTEM" refreshTrigger={refreshTrigger} />
                </CardContent>
            </Card>
        </div>
      </div>
    </div>
  );
}
