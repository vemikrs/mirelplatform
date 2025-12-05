import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  SectionHeading,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Input,
  Badge,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@mirel/ui';
import { 
  Building, 
  Search, 
  Plus, 
  MoreHorizontal, 
  Settings,
  Power,
  ExternalLink,
  Loader2
} from 'lucide-react';
import { toast } from '@mirel/ui';
import { getTenants, updateTenantStatus } from '../api/tenant';
import type { TenantStatus } from '../api/tenant';

export function TenantManagementPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const queryClient = useQueryClient();

  const { data: tenants = [], isLoading } = useQuery({
    queryKey: ['admin-tenants'],
    queryFn: getTenants,
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: TenantStatus }) => 
      updateTenantStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
      toast({
        title: 'ステータスを更新しました',
        variant: 'success',
      });
    },
    onError: () => {
      toast({
        title: 'ステータスの更新に失敗しました',
        variant: 'destructive',
      });
    }
  });

  const handleStatusChange = (id: string, currentStatus: TenantStatus) => {
    const newStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    updateStatusMutation.mutate({ id, status: newStatus });
  };

  const filteredTenants = tenants.filter(tenant => 
    tenant.tenantName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    tenant.domain.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6 pb-16">
      <SectionHeading
        eyebrow={
          <span className="inline-flex items-center gap-2">
            <Building className="size-4" />
            システム管理
          </span>
        }
        title="テナント管理"
        description="マルチテナント環境におけるテナントの作成、設定、ライフサイクルを管理します。"
        actions={
          <Button>
            <Plus className="size-4 mr-2" />
            新規テナント作成
          </Button>
        }
      />

      <Card>
        <CardHeader className="py-4 px-6 border-b">
          <div className="flex items-center justify-between gap-4">
            <CardTitle className="text-base">登録テナント一覧</CardTitle>
            <div className="relative w-72">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="テナント名またはドメインで検索..."
                className="pl-9 h-9"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="relative w-full overflow-auto">
            {isLoading ? (
              <div className="p-8 flex justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              </div>
            ) : (
            <table className="w-full caption-bottom text-sm">
              <thead className="[&_tr]:border-b">
                <tr className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                  <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                    テナント名 / ドメイン
                  </th>
                  <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                    プラン
                  </th>
                  <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                    ステータス
                  </th>
                  <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                    管理者 / ユーザー数
                  </th>
                  <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                    登録日
                  </th>
                  <th className="h-12 px-4 align-middle font-medium text-muted-foreground text-right">
                    アクション
                  </th>
                </tr>
              </thead>
              <tbody className="[&_tr:last-child]:border-0">
                {filteredTenants.length > 0 ? (
                  filteredTenants.map((tenant) => (
                    <tr
                      key={tenant.tenantId}
                      className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted"
                    >
                      <td className="p-4 align-middle">
                        <div className="flex flex-col">
                          <span className="font-medium">{tenant.tenantName}</span>
                          <span className="text-xs text-muted-foreground font-mono">{tenant.domain}</span>
                        </div>
                      </td>
                      <td className="p-4 align-middle">
                        <Badge variant="outline">{tenant.plan}</Badge>
                      </td>
                      <td className="p-4 align-middle">
                        <Badge 
                          variant={tenant.status === 'ACTIVE' ? 'success' : 'destructive'}
                        >
                          {tenant.status}
                        </Badge>
                      </td>
                      <td className="p-4 align-middle">
                        <div className="flex flex-col">
                          <span className="text-sm">{tenant.adminUser || '-'}</span>
                          <span className="text-xs text-muted-foreground">{tenant.userCount || 0} Users</span>
                        </div>
                      </td>
                      <td className="p-4 align-middle">
                        <div className="text-xs text-muted-foreground">
                        {tenant.createdAt ? new Date(tenant.createdAt).toLocaleDateString() : '-'}
                        </div>
                      </td>
                      <td className="p-4 align-middle text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                              <span className="sr-only">Open menu</span>
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuLabel>操作</DropdownMenuLabel>
                            <DropdownMenuItem>
                              <ExternalLink className="mr-2 h-4 w-4" />
                              テナントへ移動
                            </DropdownMenuItem>
                            <DropdownMenuItem>
                              <Settings className="mr-2 h-4 w-4" />
                              設定変更
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              className={tenant.status === 'ACTIVE' ? 'text-destructive' : 'text-green-600'}
                              onClick={() => handleStatusChange(tenant.tenantId, tenant.status)}
                            >
                              <Power className="mr-2 h-4 w-4" />
                              {tenant.status === 'ACTIVE' ? '停止' : '再開'}
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="p-8 text-center text-muted-foreground">
                      該当するテナントが見つかりません。
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
