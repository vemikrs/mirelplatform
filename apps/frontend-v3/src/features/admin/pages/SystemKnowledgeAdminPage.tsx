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
import { FileText, Loader2, ShieldAlert } from 'lucide-react';

export default function SystemKnowledgeAdminPage() {
  const { toast } = useToast();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);

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

      <div className="grid gap-6 md:grid-cols-2">
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
             <div className="grid w-full items-center gap-1.5">
              <Label htmlFor="file">ドキュメントファイル</Label>
              <Input id="file" type="file" onChange={handleFileChange} disabled={isUploading} />
            </div>

            {selectedFile && (
                <div className="bg-muted p-2 rounded text-sm flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    {selectedFile.name}
                    <Badge variant="destructive" className="ml-auto">SYSTEM GLOBAL</Badge>
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

        <Card>
           <CardHeader>
            <CardTitle>登録済みドキュメント</CardTitle>
            <CardDescription>
              現在システムスコープで登録されているドキュメント一覧
            </CardDescription>
          </CardHeader>
          <CardContent>
             <div className="text-center text-muted-foreground py-8">
                一覧表示機能は実装中です。
             </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
