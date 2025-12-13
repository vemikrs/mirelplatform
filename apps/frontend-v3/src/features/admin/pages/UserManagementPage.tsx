import {
  Badge,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
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
  toast,
} from '@mirel/ui';
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { Edit2, MoreHorizontal, Search, Trash2, UserPlus } from 'lucide-react';
import { useState } from 'react';
import { getUsers, deleteUser, createUser, updateUser } from '../api';
import type { AdminUser, CreateUserRequest, UpdateUserRequest } from '../api';
import { getApiErrors } from '@/lib/api/client';
import { UserFormDialog } from '../components/UserFormDialog';



export const UserManagementPage = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [userToDelete, setUserToDelete] = useState<AdminUser | null>(null);
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
      toast({
        title: '削除完了',
        description: 'ユーザーを削除しました。',
      });
    },
    onError: (error) => {
      console.error("Failed to delete user", error);
      const errors = getApiErrors(error);
      toast({
        title: '削除失敗',
        description: errors.join('\n'),
        variant: 'destructive',
      });
    }
  });

  // Create mutation
  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setIsDialogOpen(false);
      setSelectedUser(null);
      toast({
        title: '作成完了',
        description: 'ユーザーを作成しました。',
      });
    },
    onError: (error) => {
      console.error("Failed to create user", error);
      const errors = getApiErrors(error);
      toast({
        title: '作成失敗',
        description: errors.join('\n'),
        variant: 'destructive',
      });
    }
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: UpdateUserRequest }) => 
      updateUser(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setIsDialogOpen(false);
      setSelectedUser(null);
      toast({
        title: '更新完了',
        description: 'ユーザー情報を更新しました。',
      });
    },
    onError: (error) => {
      console.error("Failed to update user", error);
      const errors = getApiErrors(error);
      toast({
        title: '更新失敗',
        description: errors.join('\n'),
        variant: 'destructive',
      });
    }
  });

  const handleDelete = (user: AdminUser) => {
    setUserToDelete(user);
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    if (!userToDelete) return;
    try {
      await deleteMutation.mutateAsync(userToDelete.userId);
      setShowDeleteConfirm(false);
      setUserToDelete(null);
    } catch {
      // エラーはonErrorで処理済み
    }
  };

  const handleDeleteFromDialog = async (userId: string) => {
    try {
      await deleteMutation.mutateAsync(userId);
      setIsDialogOpen(false);
      setSelectedUser(null);
    } catch {
      // エラー時はダイアログを閉じず、toastで通知（onErrorで既に通知済み）
    }
  };

  const handleCreateUser = () => {
    setSelectedUser(null);
    setIsDialogOpen(true);
  };

  const handleEditUser = (user: AdminUser) => {
    setSelectedUser(user);
    setIsDialogOpen(true);
  };

  const handleFormSubmit = (data: CreateUserRequest | UpdateUserRequest) => {
    if (selectedUser) {
      updateMutation.mutate({ 
        userId: selectedUser.userId, 
        data: data as UpdateUserRequest 
      });
    } else {
      createMutation.mutate(data as CreateUserRequest);
    }
  };

  const handleTenantUpdateSuccess = (updatedUser: AdminUser) => {
    queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    setSelectedUser(updatedUser);
  };

  const handleDialogClose = () => {
    setIsDialogOpen(false);
    setSelectedUser(null);
  };

  return (
    <div className="space-y-4 sm:space-y-6 p-4 sm:p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">ユーザー・ロール管理</h1>
        <Button onClick={handleCreateUser}>
          <UserPlus className="mr-2 h-4 w-4" />
          新規ユーザー作成
        </Button>
      </div>

      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 sm:gap-4">
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

      {/* デスクトップ: テーブル表示 */}
      <div className="hidden md:block rounded-md border overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[120px]">ユーザー名</TableHead>
              <TableHead>名前</TableHead>
              <TableHead>メールアドレス</TableHead>
              <TableHead>ロール</TableHead>
              <TableHead>ステータス</TableHead>
              <TableHead className="min-w-[150px]">最終ログイン</TableHead>
              <TableHead className="w-20"></TableHead>
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
                    <div className="flex gap-1 flex-wrap">
                        {(user.roles || '').split(/[|,]/).filter(Boolean).map((role: string) => (
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
                    <TableCell className="whitespace-nowrap">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('ja-JP') : '-'}</TableCell>
                    <TableCell>
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                            <span className="sr-only">メニューを開く</span>
                            <MoreHorizontal className="h-4 w-4" />
                        </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => handleEditUser(user)}>
                            <Edit2 className="mr-2 h-4 w-4" />
                            編集
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                            className="text-red-600"
                            onClick={() => handleDelete(user)}
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

      {/* モバイル: カード表示 */}
      <div className="md:hidden space-y-3">
        {isLoading ? (
          <div className="flex items-center justify-center h-24 text-sm text-muted-foreground">
            読み込み中...
          </div>
        ) : users.length === 0 ? (
          <div className="flex items-center justify-center h-24 text-sm text-muted-foreground">
            ユーザーが見つかりません
          </div>
        ) : (
          users.map((user: AdminUser) => (
            <div key={user.userId} className="rounded-lg border bg-card p-3 space-y-2">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <h3 className="font-semibold truncate">{user.displayName}</h3>
                    <Badge variant={user.isActive ? 'success' : 'outline'} size="sm">
                      {user.isActive ? '有効' : '無効'}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground truncate">@{user.username}</p>
                  <p className="text-sm text-muted-foreground truncate">{user.email}</p>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="h-8 w-8 p-0 shrink-0">
                      <span className="sr-only">メニューを開く</span>
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => handleEditUser(user)}>
                      <Edit2 className="mr-2 h-4 w-4" />
                      編集
                    </DropdownMenuItem>
                    <DropdownMenuItem 
                      className="text-red-600"
                      onClick={() => handleDelete(user)}
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      削除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
              
              <div className="flex flex-wrap gap-1">
                {(user.roles || '').split(/[|,]/).filter(Boolean).map((role: string) => (
                  <Badge
                    key={role}
                    variant={role === 'ADMIN' ? 'destructive' : 'neutral'}
                    size="sm"
                  >
                    {role}
                  </Badge>
                ))}
              </div>

              <div className="text-xs text-muted-foreground pt-2 border-t">
                最終ログイン: {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('ja-JP', { 
                  month: 'short', 
                  day: 'numeric', 
                  hour: '2-digit', 
                  minute: '2-digit' 
                }) : '未ログイン'}
              </div>
            </div>
          ))
        )}
      </div>

      <UserFormDialog
        open={isDialogOpen}
        onClose={handleDialogClose}
        onSubmit={handleFormSubmit}
        onDelete={handleDeleteFromDialog}
        onTenantUpdateSuccess={handleTenantUpdateSuccess}
        user={selectedUser}
        isLoading={createMutation.isPending || updateMutation.isPending || deleteMutation.isPending}
      />

      {/* 削除確認ダイアログ */}
      <Dialog open={showDeleteConfirm} onOpenChange={setShowDeleteConfirm}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>ユーザーの削除</DialogTitle>            <DialogDescription>
              ユーザー {userToDelete?.displayName} を削除してもよろしいですか? この操作は取り消せません。
            </DialogDescription>          </DialogHeader>
          <p className="text-sm text-muted-foreground py-4">
            ユーザー <strong className="text-foreground">{userToDelete?.displayName}</strong> を削除してもよろしいですか？
            <span className="block text-sm text-destructive mt-2">
              この操作は取り消せません。
            </span>
          </p>
          <DialogFooter>
            <Button 
              type="button" 
              variant="outline" 
              onClick={() => {
                setShowDeleteConfirm(false);
                setUserToDelete(null);
              }}
              disabled={deleteMutation.isPending}
            >
              キャンセル
            </Button>
            <Button 
              type="button" 
              variant="destructive" 
              onClick={confirmDelete}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? '削除中...' : '削除する'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
