import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Card, CardContent, Input, Label, Textarea, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Switch } from '@mirel/ui';
import { getAnnouncement, createAnnouncement, updateAnnouncement, type AnnouncementSaveDto } from '@/lib/api/announcement-admin';

const INITIAL_DATA: AnnouncementSaveDto = {
  title: '',
  content: '',
  summary: '',
  category: 'INFO',
  priority: 'NORMAL',
  status: 'DRAFT',
  isPinned: false,
  requiresAcknowledgment: false,
  targets: [{ targetType: 'ALL', targetId: 'ALL' }],
};

export default function AnnouncementEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isNew = !id || id === 'new';
  const [formData, setFormData] = useState<AnnouncementSaveDto>(INITIAL_DATA);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-announcement', id],
    queryFn: () => getAnnouncement(id!),
    enabled: !isNew,
  });

  useEffect(() => {
    if (data?.announcement) {
      setFormData({
        ...data.announcement,
        targets: data.targets || [],
      });
    }
  }, [data]);

  const saveMutation = useMutation({
    mutationFn: (dto: AnnouncementSaveDto) => isNew ? createAnnouncement(dto) : updateAnnouncement(id!, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-announcements'] });
      navigate('/admin/announcements');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    saveMutation.mutate(formData);
  };

  if (isLoading) return <div>Loading...</div>;

  return (
    <div className="container py-4">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">{isNew ? 'お知らせ作成' : 'お知らせ編集'}</h1>
      </div>

      <form onSubmit={handleSubmit}>
        <Card>
          <CardContent className="space-y-4 pt-4">
            <div className="space-y-1.5">
              <Label>タイトル</Label>
              <Input
                required
                value={formData.title}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, title: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>カテゴリ</Label>
                <Select value={formData.category} onValueChange={(v: string) => setFormData({ ...formData, category: v })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="INFO">INFO</SelectItem>
                    <SelectItem value="RELEASE">RELEASE</SelectItem>
                    <SelectItem value="MAINTENANCE">MAINTENANCE</SelectItem>
                    <SelectItem value="ALERT">ALERT</SelectItem>
                    <SelectItem value="TERMS">TERMS</SelectItem>
                    <SelectItem value="EVENT">EVENT</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>優先度</Label>
                <Select value={formData.priority} onValueChange={(v: string) => setFormData({ ...formData, priority: v })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">LOW</SelectItem>
                    <SelectItem value="NORMAL">NORMAL</SelectItem>
                    <SelectItem value="HIGH">HIGH</SelectItem>
                    <SelectItem value="CRITICAL">CRITICAL</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>概要</Label>
              <Input
                value={formData.summary || ''}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, summary: e.target.value })}
              />
            </div>

            <div className="space-y-1.5">
              <Label>本文 (Markdown)</Label>
              <Textarea
                className="min-h-[200px]"
                value={formData.content}
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setFormData({ ...formData, content: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>ステータス</Label>
                <Select value={formData.status} onValueChange={(v: string) => setFormData({ ...formData, status: v })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DRAFT">下書き</SelectItem>
                    <SelectItem value="PUBLISHED">公開</SelectItem>
                    <SelectItem value="ARCHIVED">アーカイブ</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              
              <div className="space-y-1.5">
                <Label>公開日時</Label>
                <Input
                  type="datetime-local"
                  value={formData.publishAt ? new Date(formData.publishAt).toISOString().slice(0, 16) : ''}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, publishAt: e.target.value ? new Date(e.target.value).toISOString() : undefined })}
                />
              </div>
            </div>

            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <Switch
                  checked={formData.isPinned}
                  onCheckedChange={(c: boolean) => setFormData({ ...formData, isPinned: c })}
                />
                <Label>ピン留め</Label>
              </div>
            </div>

            <div className="flex justify-end space-x-2 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate('/admin/announcements')}>
                キャンセル
              </Button>
              <Button type="submit" disabled={saveMutation.isPending}>
                保存
              </Button>
            </div>
          </CardContent>
        </Card>
      </form>
    </div>
  );
}
