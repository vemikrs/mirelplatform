
import {
  Badge,
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  Input,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@mirel/ui';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Edit2, MoreHorizontal, Plus, Search } from 'lucide-react';
import { useState } from 'react';
import { getTenants, createTenant } from '../api';
import type { Tenant, TenantPlan } from '../api';

export const TenantManagementPage = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');

  // Query for tenants
  const { data: tenantsResponse, isLoading, error } = useQuery({
    queryKey: ['admin-tenants', searchQuery],
    queryFn: async () => {
      // Direct API call or wrapper - let's use wrapper if it returns data, otherwise fix wrapper usage
      // Assuming getTenants returns AxiosResponse based on previous errors
      const res = await getTenants(searchQuery || undefined);
      return res.data; 
    },
  });

  const tenants = tenantsResponse || [];

  // Create mutation
  const createMutation = useMutation({
    mutationFn: createTenant,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-tenants'] });
    }
  });

  const handleCreateTenant = () => {
    // Stub for create dialog
    const name = prompt("Enter tenant name");
    if (name) {
        const domain = prompt("Enter domain (unique)");
        if (domain) {
            createMutation.mutate({
                tenantName: name,
                domain: domain,
                plan: 'FREE' as TenantPlan // default
            });
        }
    }
  };

  if (isLoading) return <div>読み込み中...</div>;
  if (error) return <div>エラーが発生しました</div>;

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">テナント管理</h1>
        <Button onClick={handleCreateTenant}>
          <Plus className="mr-2 h-4 w-4" />
          新規テナント作成
        </Button>
      </div>

      <div className="flex items-center space-x-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="テナント名またはドメインで検索..."
            className="pl-8"
            value={searchQuery}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>テナント名</TableHead>
              <TableHead>ドメイン</TableHead>
              <TableHead>プラン</TableHead>
              <TableHead>ステータス</TableHead>
              <TableHead className="text-right">ユーザー数</TableHead>
              <TableHead>作成日</TableHead>
              <TableHead className="w-[80px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {tenants.length === 0 ? (
                <TableRow>
                    <TableCell colSpan={7} className="h-24 text-center">
                        テナントが見つかりません
                    </TableCell>
                </TableRow>
            ) : (
                tenants.map((tenant: Tenant) => (
                <TableRow key={tenant.tenantId}>
                    <TableCell className="font-medium">{tenant.tenantName}</TableCell>
                    <TableCell>{tenant.domain}</TableCell>
                    <TableCell>
                    <Badge variant="outline">{tenant.plan}</Badge>
                    </TableCell>
                    <TableCell>
                    <Badge variant={tenant.status === 'ACTIVE' ? 'success' : 'neutral'}>
                        {tenant.status}
                    </Badge>
                    </TableCell>
                    <TableCell className="text-right">{tenant.userCount}</TableCell>
                    <TableCell>{new Date(tenant.createdAt).toLocaleDateString()}</TableCell>
                    <TableCell>
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                            <span className="sr-only">メニューを開く</span>
                            <MoreHorizontal className="h-4 w-4" />
                        </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                        <DropdownMenuItem>
                            <Edit2 className="mr-2 h-4 w-4" />
                            設定変更
                        </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                    </TableCell>
                </TableRow>
                ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};
