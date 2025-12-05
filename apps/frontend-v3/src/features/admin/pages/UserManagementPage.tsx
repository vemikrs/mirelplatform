
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
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { Edit2, MoreHorizontal, Search, Trash2, UserPlus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getUsers, deleteUser, createUser } from '../api';
import type { AdminUser } from '../api';



export const UserManagementPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  // New state variables introduced by the change
  // Query for fetching users
  const { data: userResponse, isLoading } = useQuery({
    queryKey: ['admin-users', searchQuery, roleFilter],
    queryFn: async () => {
      const res = await getUsers({ 
        q: searchQuery || undefined, 
        role: roleFilter !== 'ALL' ? roleFilter : undefined 
      });
      return res.data;
    },
    placeholderData: keepPreviousData,
  });
  
  const users = userResponse?.users || [];

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: deleteUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      // TODO: Show toast success
    },
    onError: (error) => {
      console.error("Failed to delete user", error);
      // TODO: Show toast error
    }
  });

  // Create mutation (Placeholder for full modal/form flow)
  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    }
  });

  const handleDelete = (userId: string) => {
    if (confirm('Are you sure you want to delete this user?')) {
      deleteMutation.mutate(userId);
    }
  };

  const handleCreateUser = () => {
    const username = prompt("Enter username");
    if (username) {
        createMutation.mutate({
            username: username,
            email: `${username}@example.com`,
            displayName: username,
            roles: ['USER'],
            isActive: true,
        });
    }
  };

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">ユーザー・ロール管理</h1>
        <Button onClick={handleCreateUser}>
          <UserPlus className="mr-2 h-4 w-4" />
          新規ユーザー作成
        </Button>
      </div>

      <div className="flex items-center space-x-4">
        <div className="relative flex-1">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="名前またはメールアドレスで検索..."
            className="pl-8"
            value={searchQuery}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="w-[200px]">
          <select
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            value={roleFilter}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => setRoleFilter(e.target.value)}
          >
            <option value="ALL">全てのロール</option>
            <option value="ADMIN">管理者 (ADMIN)</option>
            <option value="USER">一般ユーザー (USER)</option>
          </select>
        </div>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[100px]">ID</TableHead>
              <TableHead>名前</TableHead>
              <TableHead>メールアドレス</TableHead>
              <TableHead>ロール</TableHead>
              <TableHead>ステータス</TableHead>
              <TableHead>最終ログイン</TableHead>
              <TableHead className="w-[80px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
                <TableRow>
                    <TableCell colSpan={7} className="h-24 text-center">
                        読み込み中...
                    </TableCell>
                </TableRow>
            ) : users.length === 0 ? (
                <TableRow>
                    <TableCell colSpan={7} className="h-24 text-center">
                        ユーザーが見つかりません
                    </TableCell>
                </TableRow>
            ) : (
                users.map((user: AdminUser) => (
                <TableRow key={user.userId}>
                    <TableCell className="font-medium">{user.username}</TableCell>
                    <TableCell>{user.displayName}</TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                    <div className="flex gap-1">
                        {user.roles.split(',').map((role: string) => (
                        <Badge
                            key={role}
                            variant={role === 'ADMIN' ? 'destructive' : 'neutral'}
                        >
                            {role}
                        </Badge>
                        ))}
                    </div>
                    </TableCell>
                    <TableCell>
                    <Badge variant={user.isActive ? 'success' : 'outline'}>
                        {user.isActive ? '有効' : '無効'}
                    </Badge>
                    </TableCell>
                    <TableCell>{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : '-'}</TableCell>
                    <TableCell>
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                            <span className="sr-only">メニューを開く</span>
                            <MoreHorizontal className="h-4 w-4" />
                        </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => navigate(`${user.userId}`)}>
                            <Edit2 className="mr-2 h-4 w-4" />
                            編集
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                            className="text-red-600"
                            onClick={() => handleDelete(user.userId)}
                        >
                            <Trash2 className="mr-2 h-4 w-4" />
                            削除
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
