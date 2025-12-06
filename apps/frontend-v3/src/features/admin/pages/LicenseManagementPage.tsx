
import {
  SectionHeading,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Input,
  Badge,
} from '@mirel/ui';
import { 
  CreditCard, 
  HardDrive, 
  Users, 
  Activity, 
  CheckCircle, 
  AlertTriangle, 
  RefreshCw 
} from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getLicenseInfo, updateLicenseKey } from '../api';

import { useState } from 'react';

export function LicenseManagementPage() {
  const queryClient = useQueryClient();
  const [keyInput, setKeyInput] = useState('');

  const { data: license, isLoading } = useQuery({
    queryKey: ['admin-license'],
    queryFn: async () => {
        const res = await getLicenseInfo();
        return res.data;
    },
  });

  const updateKeyMutation = useMutation({
    mutationFn: updateLicenseKey,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-license'] });
      setKeyInput('');
      // TODO: Toast
    }
  });

  const handleUpdateKey = () => {
    if (keyInput) {
        updateKeyMutation.mutate(keyInput);
    }
  };

  if (isLoading) {
    return <div className="p-6">読み込み中...</div>;
  }

  // Fallback defaults if API returns null/empty (should depend on API contract)
  const info = license || {
    plan: 'UNKNOWN',
    status: 'UNKNOWN',
    expiryDate: '-',
    currentUsers: 0,
    maxUsers: 1,
    currentStorage: 0,
    maxStorage: 1,
    licenseKey: '****'
  };

  const userUsagePercent = Math.min(100, Math.round((info.currentUsers / info.maxUsers) * 100));
  const storageUsagePercent = Math.min(100, Math.round((info.currentStorage / info.maxStorage) * 100));

  return (
    <div className="space-y-6 pb-16">
      <SectionHeading
        eyebrow={
          <span className="inline-flex items-center gap-2">
            <CreditCard className="size-4" />
            システム管理
          </span>
        }
        title="ライセンス管理"
        description="システム全体のライセンス状況、有効期限、リソース使用状況を確認します。"
      />

      {/* Status Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">現在のプラン</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{info.plan}</div>
            <p className="text-xs text-muted-foreground">
              Pro Edition
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">ステータス</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-bold">{info.status}</span>
              {info.status === 'ACTIVE' && <CheckCircle className="h-4 w-4 text-green-500" />}
            </div>
            <p className="text-xs text-muted-foreground">
              正常稼働中
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">有効期限</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{info.expiryDate}</div>
            <p className="text-xs text-muted-foreground">
              あと 320 日
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">テナント数</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">12 / 20</div>
            <p className="text-xs text-muted-foreground">
              プラン上限まで 8
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Resource Usage */}
        <Card>
          <CardHeader>
            <CardTitle>リソース使用状況</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  <span>ユーザー数</span>
                </div>
                <span className="font-medium">{info.currentUsers} / {info.maxUsers}</span>
              </div>
              <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                <div className="h-full bg-primary" style={{ width: `${userUsagePercent}%` }} />
              </div>
              <p className="text-xs text-muted-foreground text-right">{userUsagePercent}% 使用中</p>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <HardDrive className="h-4 w-4 text-muted-foreground" />
                  <span>ストレージ</span>
                </div>
                <span className="font-medium">{info.currentStorage} GB / {info.maxStorage} GB</span>
              </div>
              <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                <div className="h-full bg-primary" style={{ width: `${storageUsagePercent}%` }} />
              </div>
              <p className="text-xs text-muted-foreground text-right">{storageUsagePercent}% 使用中</p>
            </div>
          </CardContent>
        </Card>

        {/* License Update */}
        <Card>
          <CardHeader>
            <CardTitle>ライセンス更新</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="rounded-lg bg-muted p-4">
              <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium">現在のライセンスキー</span>
                  <Badge variant="outline">AES-256</Badge>
              </div>
              <code className="text-sm font-mono block break-all">
                {info.licenseKey}
              </code>
            </div>
            
            <div className="space-y-2">
              <label className="text-sm font-medium">新しいライセンスキー</label>
              <div className="flex gap-2">
                <Input 
                    placeholder="XXXX-XXXX-XXXX-XXXX" 
                    value={keyInput}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setKeyInput(e.target.value)}
                />
                <Button onClick={handleUpdateKey}>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  更新
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                新しいライセンスキーを入力して即時適用します。変更は全てのテナントに反映されます。
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
