import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Button,
  Input,
  Label,
  Switch,
} from '@mirel/ui';
import { Trash2 } from 'lucide-react';
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
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    displayName: '',
    firstName: '',
    lastName: '',
    password: '',
    roles: [] as string[],
    isActive: true,
  });

  const [roleCheckboxes, setRoleCheckboxes] = useState({
    ADMIN: false,
    USER: false,
    TENANT_ADMIN: false,
  });

  useEffect(() => {
    if (user) {
      const userRoles = user.roles.split(/[|,]/).map(r => r.trim());
      setFormData({
        username: user.username,
        email: user.email,
        displayName: user.displayName,
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        password: '',
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
        password: '',
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
        .filter(([_, checked]) => checked)
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
        password: formData.password || undefined,
        roles: formData.roles,
        isActive: formData.isActive,
      };
      onSubmit(createData);
    }
  };

  const handleDelete = () => {
    if (!user || !onDelete) return;
    setShowDeleteConfirm(true);
  };

  const confirmDelete = () => {
    if (!user || !onDelete) return;
    onDelete(user.userId);
    setShowDeleteConfirm(false);
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{user ? 'ユーザー編集' : '新規ユーザー作成'}</DialogTitle>
        </DialogHeader>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="username">ユーザー名 *</Label>
              <Input
                id="username"
                value={formData.username}
                onChange={(e) => setFormData(prev => ({ ...prev, username: e.target.value }))}
                required
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="email">メールアドレス *</Label>
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                required
              />
            </div>
          </div>

          {!user && (
            <div className="space-y-2">
              <Label htmlFor="password">パスワード</Label>
              <Input
                id="password"
                type="password"
                value={formData.password}
                onChange={(e) => setFormData(prev => ({ ...prev, password: e.target.value }))}
                placeholder="空欄の場合は自動生成されます"
              />
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="displayName">表示名 *</Label>
            <Input
              id="displayName"
              value={formData.displayName}
              onChange={(e) => setFormData(prev => ({ ...prev, displayName: e.target.value }))}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
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

          <div className="space-y-3">
            <Label>ロール設定 *</Label>
            <div className="grid gap-2">
              {[
                { id: 'ADMIN', label: 'ADMIN', desc: 'システム全体の管理権限を持ちます' },
                { id: 'TENANT_ADMIN', label: 'TENANT_ADMIN', desc: '所属テナント内の管理権限を持ちます' },
                { id: 'USER', label: 'USER', desc: '一般的な利用権限のみを持ちます' },
              ].map((role) => (
                <div
                  key={role.id}
                  className={`flex flex-row items-center justify-between rounded-lg border p-3 shadow-xs cursor-pointer transition-all ${
                    roleCheckboxes[role.id as keyof typeof roleCheckboxes]
                      ? 'border-primary bg-primary/5'
                      : 'hover:bg-surface-raised'
                  }`}
                  onClick={() => handleRoleToggle(role.id as keyof typeof roleCheckboxes)}
                >
                  <div className="space-y-0.5">
                    <Label className="text-base font-medium cursor-pointer">
                      {role.label}
                    </Label>
                    <p className="text-xs text-muted-foreground">
                      {role.desc}
                    </p>
                  </div>
                  <Switch
                    checked={roleCheckboxes[role.id as keyof typeof roleCheckboxes]}
                    onCheckedChange={() => handleRoleToggle(role.id as keyof typeof roleCheckboxes)}
                  />
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <Switch
              id="isActive"
              checked={formData.isActive}
              onCheckedChange={(checked) => setFormData(prev => ({ ...prev, isActive: checked }))}
            />
            <Label htmlFor="isActive" className="cursor-pointer font-normal">
              アカウント有効
            </Label>
          </div>

          <DialogFooter>
            {user && onDelete && (
              <Button 
                type="button" 
                variant="destructive" 
                onClick={handleDelete} 
                disabled={isLoading}
                className="mr-auto"
              >
                <Trash2 className="mr-2 h-4 w-4" />
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

      {/* 削除確認ダイアログ */}
      <Dialog open={showDeleteConfirm} onOpenChange={setShowDeleteConfirm}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>ユーザーの削除</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <p className="text-sm text-muted-foreground">
              ユーザー <strong className="text-foreground">{user?.displayName}</strong> を削除してもよろしいですか？
            </p>
            <p className="text-sm text-destructive mt-2">
              この操作は取り消せません。
            </p>
          </div>
          <DialogFooter>
            <Button 
              type="button" 
              variant="outline" 
              onClick={() => setShowDeleteConfirm(false)}
            >
              キャンセル
            </Button>
            <Button 
              type="button" 
              variant="destructive" 
              onClick={confirmDelete}
            >
              削除する
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Dialog>
  );
};
