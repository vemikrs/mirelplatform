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
} from '@mirel/ui';
import { User as UserIcon, Mail, Shield, Building2, Calendar } from 'lucide-react';
import type { AdminUser, CreateUserRequest, UpdateUserRequest } from '../api';

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

  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

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
                      {user && user.tenants && user.tenants.length > 0 && (
                        <Badge variant="outline" size="sm">
                          {user.tenants.length}件
                        </Badge>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      テナント割り当ての編集機能は今後実装予定です
                    </p>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    {user && user.tenants && user.tenants.length > 0 ? (
                      user.tenants.map((tenant) => (
                        <div key={tenant.tenantId} className="flex items-center justify-between p-2 sm:p-3 rounded-lg border bg-surface-raised">
                          <div className="space-y-1">
                            <p className="text-sm font-medium">{tenant.tenantName}</p>
                            <div className="flex items-center gap-2">
                              <Badge variant="outline" size="sm">
                                {tenant.roleInTenant}
                              </Badge>
                              {tenant.isDefault && (
                                <Badge variant="success" size="sm">
                                  デフォルト
                                </Badge>
                              )}
                            </div>
                          </div>
                        </div>
                      ))
                    ) : (
                      <div className="text-sm text-muted-foreground text-center py-4">
                        所属テナントがありません
                      </div>
                    )}
                  </CardContent>
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
