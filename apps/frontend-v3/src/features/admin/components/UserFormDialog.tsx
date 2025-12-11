import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Button,
  Input,
  Label,
  Switch,
  Avatar,
  Badge,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Separator,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@mirel/ui';
import { User as UserIcon, Mail, Shield, Building2, Calendar } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import type { AdminUser, CreateUserRequest, UpdateUserRequest, TenantAssignment, UserTenantAssignmentRequest } from '../api';
import { getTenants, updateUserTenants } from '../api';

interface UserFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateUserRequest | UpdateUserRequest) => void;
  onDelete?: (userId: string) => void;
  user?: AdminUser | null;
  isLoading?: boolean;
}

export const UserFormDialog = ({ 
  open, 
  onClose, 
  onSubmit, 
  onDelete,
  user,
  isLoading = false 
}: UserFormDialogProps) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    displayName: '',
    firstName: '',
    lastName: '',
    roles: [] as string[],
    isActive: true,
  });

  const [roleCheckboxes, setRoleCheckboxes] = useState({
    ADMIN: false,
    USER: false,
    TENANT_ADMIN: false,
  });

  const [tenantAssignments, setTenantAssignments] = useState<Map<string, { roleInTenant: string; isDefault: boolean }>>(
    new Map()
  );
  const [isSavingTenants, setIsSavingTenants] = useState(false);

  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

  // 全テナントリストを取得
  const { data: tenantsResponse } = useQuery({
    queryKey: ['admin-tenants'],
    queryFn: async () => {
      const res = await getTenants();
      return res.data;
    },
  });
  const allTenants = tenantsResponse || [];

  useEffect(() => {
    if (!open) return;
    
    if (user) {
      const userRoles = user.roles.split(/[|,]/).map(r => r.trim());
      setFormData({
        username: user.username,
        email: user.email,
        displayName: user.displayName,
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        roles: userRoles,
        isActive: user.isActive,
      });
      setRoleCheckboxes({
        ADMIN: userRoles.includes('ADMIN'),
        USER: userRoles.includes('USER'),
        TENANT_ADMIN: userRoles.includes('TENANT_ADMIN'),
      });
      
      // テナント割り当てを初期化
      const tenantMap = new Map<string, { roleInTenant: string; isDefault: boolean }>();
      user.tenants?.forEach(t => {
        tenantMap.set(t.tenantId, { roleInTenant: t.roleInTenant, isDefault: t.isDefault });
      });
      setTenantAssignments(tenantMap);
    } else {
      setFormData({
        username: '',
        email: '',
        displayName: '',
        firstName: '',
        lastName: '',
        roles: ['USER'],
        isActive: true,
      });
      setRoleCheckboxes({
        ADMIN: false,
        USER: true,
        TENANT_ADMIN: false,
      });
      setTenantAssignments(new Map());
    }
  }, [user, open]);

  const handleRoleToggle = (role: keyof typeof roleCheckboxes) => {
    setRoleCheckboxes(prev => {
      const updated = { ...prev, [role]: !prev[role] };
      const selectedRoles = Object.entries(updated)
        .filter(([, checked]) => checked)
        .map(([roleName]) => roleName);
      setFormData(prev => ({ ...prev, roles: selectedRoles }));
      return updated;
    });
  };

  const handleTenantToggle = (tenantId: string) => {
    setTenantAssignments(prev => {
      const newMap = new Map(prev);
      if (newMap.has(tenantId)) {
        newMap.delete(tenantId);
      } else {
        // 新規追加時はUSERロールをデフォルトとし、他にデフォルトがなければデフォルトに設定
        const hasDefault = Array.from(newMap.values()).some(v => v.isDefault);
        newMap.set(tenantId, { roleInTenant: 'USER', isDefault: !hasDefault });
      }
      return newMap;
    });
  };

  const handleTenantRoleChange = (tenantId: string, role: string) => {
    setTenantAssignments(prev => {
      const newMap = new Map(prev);
      const current = newMap.get(tenantId);
      if (current) {
        newMap.set(tenantId, { ...current, roleInTenant: role });
      }
      return newMap;
    });
  };

  const handleTenantDefaultChange = (tenantId: string) => {
    setTenantAssignments(prev => {
      const newMap = new Map(prev);
      // すべてのデフォルトを解除してから、選択されたテナントをデフォルトに
      newMap.forEach((value, key) => {
        newMap.set(key, { ...value, isDefault: key === tenantId });
      });
      return newMap;
    });
  };

  const handleSaveTenants = async () => {
    if (!user) return;
    
    const tenants: TenantAssignment[] = Array.from(tenantAssignments.entries()).map(([tenantId, data]) => ({
      tenantId,
      roleInTenant: data.roleInTenant,
      isDefault: data.isDefault,
    }));

    if (tenants.length === 0) {
      alert('少なくとも1つのテナントを選択してください');
      return;
    }

    const hasDefault = tenants.some(t => t.isDefault);
    if (!hasDefault) {
      alert('デフォルトテナントを1つ選択してください');
      return;
    }

    try {
      setIsSavingTenants(true);
      const request: UserTenantAssignmentRequest = { tenants };
      await updateUserTenants(user.userId, request);
      // 成功後、親コンポーネントに通知してリストを再取得
      onClose();
    } catch (error) {
      console.error('Failed to update tenants', error);
      alert('テナント割り当ての更新に失敗しました');
    } finally {
      setIsSavingTenants(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (user) {
      const updateData: UpdateUserRequest = {
        displayName: formData.displayName,
        firstName: formData.firstName || undefined,
        lastName: formData.lastName || undefined,
        roles: formData.roles.join(','),
        isActive: formData.isActive,
      };
      onSubmit(updateData);
    } else {
      const createData: CreateUserRequest = {
        username: formData.username,
        email: formData.email,
        displayName: formData.displayName,
        firstName: formData.firstName || undefined,
        lastName: formData.lastName || undefined,
        roles: formData.roles,
        isActive: formData.isActive,
      };
      onSubmit(createData);
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onClose}>
        <DialogContent className="max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">
          <DialogHeader className="px-4 sm:px-6 pt-4 sm:pt-6">
            <div className="flex items-center gap-4">
              {user && (
                <Avatar 
                  src={user.avatarUrl} 
                  alt={user.displayName}
                  fallback={user.displayName.charAt(0).toUpperCase()}
                  className="h-16 w-16"
                />
              )}
              <div className="flex-1">
                <DialogTitle className="text-2xl">{user ? 'ユーザー編集' : '新規ユーザー作成'}</DialogTitle>
                <DialogDescription>
                  {user ? 'ユーザー情報を編集します。' : '新しいユーザーを作成します。作成後、セットアップリンクがメールで送信され、ユーザー自身がパスワードを設定します。'}
                </DialogDescription>
              </div>
            </div>
          </DialogHeader>
          
          <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto px-4 sm:px-6 pb-4 sm:pb-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6 mt-4 sm:mt-6">
              {/* 左カラム: 基本情報 */}
              <div className="space-y-4 sm:space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base flex items-center gap-2">
                      <UserIcon className="h-4 w-4" />
                      基本情報
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="username">ユーザー名 *</Label>
                      <Input
                        id="username"
                        value={formData.username}
                        onChange={(e) => setFormData(prev => ({ ...prev, username: e.target.value }))}
                        disabled={!!user}
                        required
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="email">メールアドレス *</Label>
                      <div className="relative">
                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="email"
                          type="email"
                          className="pl-10"
                          value={formData.email}
                          onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                          disabled={!!user}
                          required
                        />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="displayName">表示名 *</Label>
                      <Input
                        id="displayName"
                        value={formData.displayName}
                        onChange={(e) => setFormData(prev => ({ ...prev, displayName: e.target.value }))}
                        required
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                      <div className="space-y-2">
                        <Label htmlFor="lastName">姓</Label>
                        <Input
                          id="lastName"
                          value={formData.lastName}
                          onChange={(e) => setFormData(prev => ({ ...prev, lastName: e.target.value }))}
                        />
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor="firstName">名</Label>
                        <Input
                          id="firstName"
                          value={formData.firstName}
                          onChange={(e) => setFormData(prev => ({ ...prev, firstName: e.target.value }))}
                        />
                      </div>
                    </div>

                    <Separator />

                    <div className="flex items-center justify-between">
                      <div className="space-y-0.5">
                        <Label htmlFor="isActive" className="cursor-pointer font-medium">
                          アカウント状態
                        </Label>
                        <p className="text-xs text-muted-foreground">
                          無効にするとログインできなくなります
                        </p>
                      </div>
                      <Switch
                        id="isActive"
                        checked={formData.isActive}
                        onCheckedChange={(checked) => setFormData(prev => ({ ...prev, isActive: checked }))}
                      />
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* 右カラム: ロール・テナント情報 */}
              <div className="space-y-4 sm:space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base flex items-center gap-2">
                      <Shield className="h-4 w-4" />
                      ロール設定
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    {[
                      { id: 'ADMIN', label: 'ADMIN', desc: 'システム全体の管理権限を持ちます', color: 'destructive' },
                      { id: 'TENANT_ADMIN', label: 'TENANT_ADMIN', desc: '所属テナント内の管理権限を持ちます', color: 'warning' },
                      { id: 'USER', label: 'USER', desc: '一般的な利用権限のみを持ちます', color: 'neutral' },
                    ].map((role) => (
                      <div
                        key={role.id}
                        className={`flex flex-row items-center justify-between rounded-lg border p-2 sm:p-3 shadow-xs transition-all ${
                          roleCheckboxes[role.id as keyof typeof roleCheckboxes]
                            ? 'border-primary bg-primary/5'
                            : 'hover:bg-surface-raised'
                        }`}
                      >
                        <label 
                          htmlFor={`role-${role.id}`}
                          className="flex-1 space-y-0.5 cursor-pointer"
                        >
                          <div className="text-sm font-medium">
                            {role.label}
                          </div>
                          <p className="text-xs text-muted-foreground">
                            {role.desc}
                          </p>
                        </label>
                        <Switch
                          id={`role-${role.id}`}
                          checked={roleCheckboxes[role.id as keyof typeof roleCheckboxes]}
                          onCheckedChange={() => handleRoleToggle(role.id as keyof typeof roleCheckboxes)}
                        />
                      </div>
                    ))}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="text-base flex items-center gap-2">
                        <Building2 className="h-4 w-4" />
                        所属テナント
                      </CardTitle>
                      {tenantAssignments.size > 0 && (
                        <Badge variant="outline" size="sm">
                          {tenantAssignments.size}件
                        </Badge>
                      )}
                    </div>
                    {user && (
                      <p className="text-xs text-muted-foreground mt-1">
                        チェックボックスでテナントを選択し、ロールとデフォルトを設定してください
                      </p>
                    )}
                  </CardHeader>
                  <CardContent className="space-y-2 max-h-64 overflow-y-auto">
                    {user ? (
                      allTenants.length > 0 ? (
                        allTenants.map((tenant) => {
                          const isAssigned = tenantAssignments.has(tenant.tenantId);
                          const assignment = tenantAssignments.get(tenant.tenantId);
                          return (
                            <div
                              key={tenant.tenantId}
                              className={`flex flex-col gap-2 p-2 sm:p-3 rounded-lg border transition-all ${
                                isAssigned ? 'border-primary bg-primary/5' : 'hover:bg-surface-raised'
                              }`}
                            >
                              <div className="flex items-center justify-between">
                                <label htmlFor={`tenant-${tenant.tenantId}`} className="flex items-center gap-2 cursor-pointer flex-1">
                                  <input
                                    id={`tenant-${tenant.tenantId}`}
                                    type="checkbox"
                                    checked={isAssigned}
                                    onChange={() => handleTenantToggle(tenant.tenantId)}
                                    className="h-4 w-4"
                                  />
                                  <span className="text-sm font-medium">{tenant.tenantName}</span>
                                </label>
                              </div>
                              {isAssigned && assignment && (
                                <div className="flex items-center gap-2 ml-6">
                                  <Select value={assignment.roleInTenant} onValueChange={(v) => handleTenantRoleChange(tenant.tenantId, v)}>
                                    <SelectTrigger className="w-32 h-8 text-xs">
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="USER">USER</SelectItem>
                                      <SelectItem value="ADMIN">ADMIN</SelectItem>
                                      <SelectItem value="MEMBER">MEMBER</SelectItem>
                                    </SelectContent>
                                  </Select>
                                  <label className="flex items-center gap-1 cursor-pointer text-xs">
                                    <input
                                      type="radio"
                                      name="defaultTenant"
                                      checked={assignment.isDefault}
                                      onChange={() => handleTenantDefaultChange(tenant.tenantId)}
                                      className="h-3 w-3"
                                    />
                                    <span>デフォルト</span>
                                  </label>
                                </div>
                              )}
                            </div>
                          );
                        })
                      ) : (
                        <div className="text-sm text-muted-foreground text-center py-4">
                          利用可能なテナントがありません
                        </div>
                      )
                    ) : (
                      <div className="text-sm text-muted-foreground text-center py-4">
                        ユーザー作成後にテナントを割り当てられます
                      </div>
                    )}
                  </CardContent>
                  {user && tenantAssignments.size > 0 && (
                    <div className="px-4 pb-4">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={handleSaveTenants}
                        disabled={isSavingTenants}
                        className="w-full"
                      >
                        {isSavingTenants ? 'テナント割り当てを保存中...' : 'テナント割り当てを保存'}
                      </Button>
                    </div>
                  )}
                </Card>

                {user && (
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-base flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        アカウント情報
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3 text-sm">
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">最終ログイン</span>
                        <span className="font-medium">
                          {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('ja-JP') : '未ログイン'}
                        </span>
                      </div>
                      <Separator />
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">作成日時</span>
                        <span className="font-medium">
                          {new Date(user.createdAt).toLocaleString('ja-JP')}
                        </span>
                      </div>
                      <Separator />
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">メール認証</span>
                        <Badge variant={user.emailVerified ? 'success' : 'outline'} size="sm">
                          {user.emailVerified ? '認証済み' : '未認証'}
                        </Badge>
                      </div>
                    </CardContent>
                  </Card>
                )}
              </div>
            </div>

            <DialogFooter className="mt-6 gap-2 sm:gap-0">
              {user && onDelete && (
                <Button
                  type="button"
                  variant="destructive"
                  onClick={() => setDeleteConfirmOpen(true)}
                  disabled={isLoading}
                  className="sm:mr-auto"
                >
                  削除
                </Button>
              )}
              <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
                キャンセル
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? '保存中...' : (user ? '更新' : '作成')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={deleteConfirmOpen} onOpenChange={setDeleteConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>ユーザー削除の確認</DialogTitle>
            <DialogDescription>
              本当にユーザー {user?.displayName} を削除してもよろしいですか？<br />
              この操作は取り消せません。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteConfirmOpen(false)}
            >
              キャンセル
            </Button>
            <Button
              variant="destructive"
              onClick={() => {
                if (user && onDelete) {
                  onDelete(user.userId);
                  setDeleteConfirmOpen(false);
                }
              }}
            >
              削除する
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};
