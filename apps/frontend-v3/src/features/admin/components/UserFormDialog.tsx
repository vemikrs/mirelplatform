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
import type { AdminUser, CreateUserRequest, UpdateUserRequest } from '../api';

interface UserFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateUserRequest | UpdateUserRequest) => void;
  user?: AdminUser | null;
  isLoading?: boolean;
}

export const UserFormDialog = ({ 
  open, 
  onClose, 
  onSubmit, 
  user,
  isLoading = false 
}: UserFormDialogProps) => {
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
      const userRoles = user.roles.split(',').map(r => r.trim());
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

          <div className="space-y-2">
            <Label>ロール *</Label>
            <div className="space-y-2">
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="role-admin"
                  checked={roleCheckboxes.ADMIN}
                  onChange={() => handleRoleToggle('ADMIN')}
                  className="h-4 w-4 rounded border-gray-300"
                />
                <Label htmlFor="role-admin" className="cursor-pointer font-normal">
                  ADMIN (システム管理者)
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="role-user"
                  checked={roleCheckboxes.USER}
                  onChange={() => handleRoleToggle('USER')}
                  className="h-4 w-4 rounded border-gray-300"
                />
                <Label htmlFor="role-user" className="cursor-pointer font-normal">
                  USER (一般ユーザー)
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="role-tenant-admin"
                  checked={roleCheckboxes.TENANT_ADMIN}
                  onChange={() => handleRoleToggle('TENANT_ADMIN')}
                  className="h-4 w-4 rounded border-gray-300"
                />
                <Label htmlFor="role-tenant-admin" className="cursor-pointer font-normal">
                  TENANT_ADMIN (テナント管理者)
                </Label>
              </div>
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
  );
};
