
import {
  SectionHeading,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardDescription,
  Button,
  Input,
  Label,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  Switch,
  Textarea,

} from '@mirel/ui';
import { 
  Settings, 
  Save, 
  RotateCcw,
  Bell,
  Shield,

  Server
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getSystemSettings, updateSystemSettings } from '../api';
import type { SystemSettings } from '../api';
import { useState, useEffect } from 'react';


export function SystemSettingsPage() {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState<SystemSettings>({});

  const { data: settings, isLoading } = useQuery({
    queryKey: ['admin-system-settings'],
    queryFn: getSystemSettings,
  });

  useEffect(() => {
    if (settings) {
      setFormData(settings);
    }
  }, [settings]);

  const updateMutation = useMutation({
    mutationFn: updateSystemSettings,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-system-settings'] });
      // TODO: toast.success('設定を保存しました');
    },
    onError: (error) => {
      console.error(error);
      // TODO: toast.error('保存に失敗しました');
    }
  });

  const handleChange = (key: string, value: string) => {
    setFormData(prev => ({ ...prev, [key]: value }));
  };

  const handleSwitchChange = (key: string, checked: boolean) => {
    setFormData(prev => ({ ...prev, [key]: checked ? 'true' : 'false' }));
  };

  const handleSave = () => {
    updateMutation.mutate(formData);
  };

  if (isLoading) return <div className="p-6">読み込み中...</div>;

  return (
    <div className="space-y-6 pb-16">
      <SectionHeading
        eyebrow={
          <span className="inline-flex items-center gap-2">
            <Settings className="size-4" />
            システム管理
          </span>
        }
        title="システム設定"
        description="アプリケーション全体に関わる設定（基本情報、セキュリティ、メンテナンスなど）を管理します。"
        actions={
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setFormData(settings || {})}>
              <RotateCcw className="size-4 mr-2" />
              変更を破棄
            </Button>
            <Button onClick={handleSave} disabled={updateMutation.isPending}>
              <Save className="size-4 mr-2" />
              {updateMutation.isPending ? '保存中...' : '設定を保存'}
            </Button>
          </div>
        }
      />

      <Tabs defaultValue="general" className="w-full">
        <TabsList className="mb-6">
          <TabsTrigger value="general" className="gap-2">
            <Server className="size-4" />
            基本設定
          </TabsTrigger>
          <TabsTrigger value="security" className="gap-2">
            <Shield className="size-4" />
            セキュリティ
          </TabsTrigger>
          <TabsTrigger value="notifications" className="gap-2">
            <Bell className="size-4" />
            通知設定
          </TabsTrigger>
          <TabsTrigger value="maintenance" className="gap-2">
            <Settings className="size-4" />
            メンテナンス
          </TabsTrigger>
        </TabsList>

        <TabsContent value="general">
          <Card>
            <CardHeader>
              <CardTitle>基本設定</CardTitle>
              <CardDescription>
                サイトの基本情報や連絡先を設定します。
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid gap-4 max-w-2xl">
                <div className="space-y-2">
                  <Label>サイト名</Label>
                  <Input 
                    value={formData['site_name'] || ''}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('site_name', e.target.value)}
                    placeholder="Mirel Platform" 
                  />
                  <p className="text-sm text-muted-foreground">ブラウザのタイトルやメールのヘッダーに使用されます。</p>
                </div>

                <div className="space-y-2">
                  <Label>管理者メールアドレス</Label>
                  <Input 
                    value={formData['admin_email'] || ''}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('admin_email', e.target.value)}
                    placeholder="admin@example.com" 
                  />
                  <p className="text-sm text-muted-foreground">システム通知や重要なアラートの送信先です。</p>
                </div>

                <div className="space-y-2">
                  <Label>サポートURL</Label>
                  <Input 
                    value={formData['support_url'] || ''}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('support_url', e.target.value)}
                    placeholder="https://support.example.com" 
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security">
          <Card>
            <CardHeader>
              <CardTitle>セキュリティ設定</CardTitle>
              <CardDescription>
                認証ポリシーやパスワード要件を設定します。
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between max-w-2xl p-4 border rounded-lg">
                <div className="space-y-0.5">
                  <Label className="text-base">二要素認証 (2FA) の強制</Label>
                  <p className="text-sm text-muted-foreground">
                    全ての管理者アカウントに対して2FAを必須にします。
                  </p>
                </div>
                <Switch 
                    checked={formData['force_2fa'] === 'true'}
                    onCheckedChange={(c: boolean) => handleSwitchChange('force_2fa', c)}
                />
              </div>

              <div className="space-y-2 max-w-2xl">
                <Label>パスワード有効期限（日）</Label>
                <Input type="number" 
                    value={formData['password_expiry_days'] || ''}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('password_expiry_days', e.target.value)}
                    placeholder="90" 
                />
              </div>

              <div className="space-y-2 max-w-2xl">
                <Label>アカウントロック回数</Label>
                <Input type="number" 
                    value={formData['account_lock_threshold'] || ''}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('account_lock_threshold', e.target.value)}
                    placeholder="5" 
                />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="maintenance">
          <Card>
            <CardHeader>
              <CardTitle>メンテナンス設定</CardTitle>
              <CardDescription>
                システムのメンテナンスモードやアクセス制限を設定します。
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between max-w-2xl p-4 border rounded-lg bg-yellow-50 dark:bg-yellow-950/20">
                <div className="space-y-0.5">
                  <Label className="text-base">メンテナンスモード</Label>
                  <p className="text-sm text-muted-foreground">
                    有効にすると、管理者以外のアクセスを遮断します。
                  </p>
                </div>
                <Switch 
                    checked={formData['maintenance_mode'] === 'true'}
                    onCheckedChange={(c: boolean) => handleSwitchChange('maintenance_mode', c)}
                />
              </div>

              <div className="space-y-2 max-w-2xl">
                <Label>メンテナンスメッセージ</Label>
                <Textarea 
                    value={formData['maintenance_message'] || ''}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => handleChange('maintenance_message', e.target.value)}
                    placeholder="現在メンテナンス中です。終了予定時刻: 12:00" 
                    rows={3}
                />
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
