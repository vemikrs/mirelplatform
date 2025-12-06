import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Button, Card, CardContent, CardHeader, CardTitle, Badge, Input } from '@mirel/ui';
import { Plus, Search, Edit, Trash2 } from 'lucide-react';
import { searchAnnouncements, deleteAnnouncement, type AnnouncementSearchCondition } from '@/lib/api/announcement-admin';
import { format } from 'date-fns';

export default function AnnouncementListPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [condition, setCondition] = useState<AnnouncementSearchCondition>({});

  const { data, isLoading } = useQuery({
    queryKey: ['admin-announcements', page, condition],
    queryFn: () => searchAnnouncements(condition, page),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAnnouncement,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-announcements'] });
    },
  });

  const handleDelete = async (id: string) => {
    if (confirm('本当に削除しますか？')) {
      deleteMutation.mutate(id);
    }
  };

  return (
    <div className="container py-4 space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">お知らせ管理</h1>
        <Button onClick={() => navigate('/admin/workspace/announcements/new')}>
          <Plus className="mr-2 size-4" />
          新規作成
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>検索条件</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4 pt-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              placeholder="タイトル"
              value={condition.title || ''}
              onChange={(e) => setCondition({ ...condition, title: e.target.value })}
            />
            {/* TODO: Category/Status Select */}
          </div>
          <div className="flex justify-end">
            <Button variant="outline" onClick={() => setCondition({})}>クリア</Button>
            <Button className="ml-2" onClick={() => setPage(0)}>
              <Search className="mr-2 size-4" />
              検索
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          <div className="rounded-md border">
            <table className="w-full text-sm">
              <thead className="bg-muted/50">
                <tr className="border-b">
                  <th className="h-12 px-4 text-left align-middle font-medium">タイトル</th>
                  <th className="h-12 px-4 text-left align-middle font-medium">カテゴリ</th>
                  <th className="h-12 px-4 text-left align-middle font-medium">ステータス</th>
                  <th className="h-12 px-4 text-left align-middle font-medium">公開日時</th>
                  <th className="h-12 px-4 text-right align-middle font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="p-4 text-center">読み込み中...</td>
                  </tr>
                ) : data?.content?.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="p-4 text-center">データがありません</td>
                  </tr>
                ) : (
                  data?.content?.map((item: { announcementId: string; title: string; category: string; status: string; publishAt?: string }) => (
                    <tr key={item.announcementId} className="border-b last:border-0 hover:bg-muted/50">
                      <td className="p-4 font-medium">{item.title}</td>
                      <td className="p-4"><Badge variant="outline">{item.category}</Badge></td>
                      <td className="p-4"><Badge variant="outline">{item.status}</Badge></td>
                      <td className="p-4">{item.publishAt ? format(new Date(item.publishAt), 'yyyy-MM-dd HH:mm') : '-'}</td>
                      <td className="p-4 text-right space-x-2">
                        <Button variant="ghost" size="sm" onClick={() => navigate(`/admin/workspace/announcements/${item.announcementId}`)}>
                          <Edit className="size-4" />
                        </Button>
                        <Button variant="ghost" size="sm" className="text-destructive" onClick={() => handleDelete(item.announcementId)}>
                          <Trash2 className="size-4" />
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          {/* TODO: Pagination */}
        </CardContent>
      </Card>
    </div>
  );
}
