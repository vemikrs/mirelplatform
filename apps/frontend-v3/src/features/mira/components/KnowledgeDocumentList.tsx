import { useState, useEffect } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Button,
  useToast,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@mirel/ui';
import { FileText, Trash2, Edit, Loader2 } from 'lucide-react';
import { KnowledgeDocumentEditDialog } from './KnowledgeDocumentEditDialog';

interface KnowledgeDocument {
  id: string;
  fileId: string;
  fileName: string;
  scope: string;
  tenantId: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
}

interface KnowledgeDocumentListProps {
  scope: 'SYSTEM' | 'TENANT' | 'USER';
  refreshTrigger: number;
}

export function KnowledgeDocumentList({ scope, refreshTrigger }: KnowledgeDocumentListProps) {
  const { toast } = useToast();
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [editFileId, setEditFileId] = useState<string | null>(null);

  const fetchDocuments = async () => {
    setIsLoading(true);
    try {
      const res = await fetch(`/api/mira/knowledge/list?scope=${scope}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (!res.ok) throw new Error('Failed to fetch documents');
      const data = await res.json();
      setDocuments(data);
    } catch (error) {
      console.error(error);
      toast({
        title: '取得エラー',
        description: 'ドキュメント一覧の取得に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, [refreshTrigger, scope]);

  const handleDelete = async () => {
    if (!deleteId) return;
    try {
      const res = await fetch(`/api/mira/knowledge/delete/${deleteId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (!res.ok) throw new Error('Delete failed');
      
      toast({
        title: '削除完了',
        description: 'ドキュメントを削除しました。',
      });
      fetchDocuments();
    } catch (error) {
      console.error(error);
      toast({
        title: '削除エラー',
        description: 'ドキュメントの削除に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setDeleteId(null);
    }
  };

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>ファイル名</TableHead>
            <TableHead>作成日時</TableHead>
            <TableHead className="text-right">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading ? (
            <TableRow>
              <TableCell colSpan={3} className="text-center py-8">
                <div className="flex justify-center items-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    読み込み中...
                </div>
              </TableCell>
            </TableRow>
          ) : documents.length === 0 ? (
            <TableRow>
              <TableCell colSpan={3} className="text-center py-8 text-muted-foreground">
                登録されたドキュメントはありません。
              </TableCell>
            </TableRow>
          ) : (
            documents.map((doc) => (
              <TableRow key={doc.id}>
                <TableCell className="font-medium flex items-center gap-2">
                  <FileText className="h-4 w-4 text-blue-500" />
                  {doc.fileName}
                </TableCell>
                <TableCell>
                  {new Date(doc.createdAt).toLocaleString()}
                </TableCell>
                <TableCell className="text-right space-x-2">
                  <Button variant="ghost" size="sm" onClick={() => setEditFileId(doc.fileId)}>
                    <Edit className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="sm" className="text-destructive" onClick={() => setDeleteId(doc.fileId)}>
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>

      <Dialog open={!!deleteId} onOpenChange={(open) => !open && setDeleteId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>本当に削除しますか？</DialogTitle>
            <DialogDescription>
              この操作は取り消せません。ナレッジベースからこのドキュメントが完全に削除されます。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteId(null)}>キャンセル</Button>
            <Button onClick={handleDelete} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">
              削除する
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {editFileId && (
        <KnowledgeDocumentEditDialog 
            fileId={editFileId} 
            open={!!editFileId} 
            onOpenChange={(open) => !open && setEditFileId(null)}
            onSave={() => {
                setEditFileId(null);
                fetchDocuments();
            }}
        />
      )}
    </div>
  );
}
