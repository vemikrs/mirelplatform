import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
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
  useToast,
} from '@mirel/ui';
import { User as UserIcon, Shield, Building2, Calendar, Trash2, Plus } from 'lucide-react';
import type { AdminUser, CreateUserRequest, UpdateUserRequest, TenantAssignment, UserTenantAssignmentRequest, Tenant } from '../api';
import { updateUserTenants } from '../api';
import { TenantSelectorDialog } from './TenantSelectorDialog';
import { cn } from '@/lib/utils/cn';

interface UserFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateUserRequest | UpdateUserRequest) => void;
  onDelete?: (userId: string) => void;
  onTenantUpdateSuccess?: (updatedUser: AdminUser) => void;
  user?: AdminUser | null;
  isLoading?: boolean;
}

type Section = 'basic' | 'roles' | 'tenants' | 'account';

interface TenantAssignmentData {
  tenantName: string;
  domain?: string;
  roleInTenant: string;
  isDefault: boolean;
}

export const UserFormDialog = ({ 
  open, 
  onClose, 
  onSubmit, 
  onDelete,
  onTenantUpdateSuccess,
  user,
  isLoading = false 
}: UserFormDialogProps) => {
  const [activeSection, setActiveSection] = useState<Section>('basic');
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

  const [tenantAssignments, setTenantAssignments] = useState<Map<string, TenantAssignmentData>>(
    new Map()
  );
  const [isSavingTenants, setIsSavingTenants] = useState(false);
  const [isTenantSelectorOpen, setIsTenantSelectorOpen] = useState(false);

  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    if (!open) {
      setActiveSection('basic');
      return;
    }
    
    if (user) {
      const userRoles = (user.roles || '').split(/[|,]/).map(r => r.trim()).filter(Boolean);
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
      
      const tenantMap = new Map<string, TenantAssignmentData>();
      user.tenants?.forEach(t => {
        tenantMap.set(t.tenantId, {
          tenantName: t.tenantName,
          roleInTenant: t.roleInTenant,
          isDefault: t.isDefault
        });
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

  const handleAddTenants = (selected: Tenant[]) => {
    setTenantAssignments(prev => {
      const newMap = new Map(prev);
      let hasDefault = Array.from(newMap.values()).some(v => v.isDefault);

      selected.forEach(t => {
        if (!newMap.has(t.tenantId)) {
          newMap.set(t.tenantId, {
            tenantName: t.tenantName,
            domain: t.domain,
            roleInTenant: 'USER',
            isDefault: !hasDefault
          });
          if (!hasDefault) hasDefault = true;
        }
      });
      return newMap;
    });
  };

  const handleRemoveTenant = (tenantId: string) => {
    setTenantAssignments(prev => {
      const newMap = new Map(prev);
      newMap.delete(tenantId);
      // If we removed the default, set another one as default if exists
      if (newMap.size > 0 && !Array.from(newMap.values()).some(v => v.isDefault)) {
        const firstKey = newMap.keys().next().value!;
        const firstVal = newMap.get(firstKey)!;
        newMap.set(firstKey, { ...firstVal, isDefault: true });
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
      toast({
        variant: 'destructive',
        title: 'エラー',
        description: '少なくとも1つのテナントを選択してください',
      });
      return;
    }

    const hasDefault = tenants.some(t => t.isDefault);
    if (!hasDefault) {
      toast({
        variant: 'destructive',
        title: 'エラー',
        description: 'デフォルトテナントを1つ選択してください',
      });
      return;
    }

    try {
      setIsSavingTenants(true);
      const request: UserTenantAssignmentRequest = { tenants };
      const res = await updateUserTenants(user.userId, request);
      
      toast({
        title: '更新完了',
        description: 'テナント割り当てを更新しました',
      });
      
      if (onTenantUpdateSuccess) {
        onTenantUpdateSuccess(res.data);
      }
    } catch (error) {
      console.error('Failed to update tenants', error);
      toast({
        variant: 'destructive',
        title: 'エラー',
        description: 'テナント割り当ての更新に失敗しました',
      });
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

  const renderSidebarItem = (id: Section, icon: React.ReactNode, label: string) => (
    <button
      type="button"
      onClick={() => setActiveSection(id)}
      className={cn(
        "w-full flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-md transition-colors",
        activeSection === id 
          ? "bg-primary/10 text-primary" 
          : "text-muted-foreground hover:bg-muted hover:text-foreground"
      )}
    >
      {icon}
      {label}
    </button>
  );

  return (
    <>
      <Dialog open={open} onOpenChange={onClose}>
        <DialogContent 
          className="max-w-5xl h-[90vh] p-0 gap-0 overflow-hidden flex flex-col md:flex-row"
          overlayClassName={isTenantSelectorOpen ? 'bg-transparent' : undefined}
        >
          
          {/* Sidebar */}
          <div className="w-full md:w-64 bg-muted/30 border-b md:border-b-0 md:border-r flex flex-col">
            <div className="p-6 pb-4">
              <div className="flex items-center gap-3 mb-1">
                <Avatar 
                  src={user?.avatarUrl} 
                  alt={user?.displayName || formData.username}
                  fallback={(user?.displayName || formData.username || 'U').charAt(0).toUpperCase()}
                  className="h-10 w-10"
                />
                <div className="min-w-0">
                  <div className="font-semibold truncate">
                    {user?.displayName || '新規ユーザー'}
                  </div>
                  <div className="text-xs text-muted-foreground truncate">
                    {user?.email || formData.email || 'メールアドレス未設定'}
                  </div>
                </div>
              </div>
            </div>
            
            <Separator />
            
            <div className="flex-1 p-4 space-y-1 overflow-y-auto">
              {renderSidebarItem('basic', <UserIcon className="h-4 w-4" />, '基本情報')}
              {renderSidebarItem('roles', <Shield className="h-4 w-4" />, 'ロール設定')}
              {renderSidebarItem('tenants', <Building2 className="h-4 w-4" />, '所属テナント')}
              {user && renderSidebarItem('account', <Calendar className="h-4 w-4" />, 'アカウント情報')}
            </div>

            <div className="p-4 border-t">
              {user && onDelete && (
                <Button
                  type="button"
                  variant="ghost"
                  className="w-full justify-start text-destructive hover:text-destructive hover:bg-destructive/10"
                  onClick={() => setDeleteConfirmOpen(true)}
                  disabled={isLoading}
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  ユーザー削除
                </Button>
              )}
            </div>
          </div>

          {/* Main Content */}
          <div className="flex-1 flex flex-col min-h-0 bg-background">
            <div className="flex-1 overflow-y-auto">
              <div className="p-6 max-w-3xl mx-auto">
                <div className="mb-6">
                  <h2 className="text-lg font-semibold">
                    {activeSection === 'basic' && '基本情報'}
                    {activeSection === 'roles' && 'ロール設定'}
                    {activeSection === 'tenants' && '所属テナント'}
                    {activeSection === 'account' && 'アカウント情報'}
                  </h2>
                  <p className="text-sm text-muted-foreground">
                    {activeSection === 'basic' && 'ユーザーの基本プロフィール情報を管理します'}
                    {activeSection === 'roles' && 'システム全体での権限レベルを設定します'}
                    {activeSection === 'tenants' && '所属するテナントとテナント内での権限を管理します'}
                    {activeSection === 'account' && 'ログイン履歴や作成日などの監査情報'}
                  </p>
                </div>

                {/* Form starts here but logic depends on section */}
                <form id="user-form" onSubmit={handleSubmit}>
                  {activeSection === 'basic' && (
                    <div className="space-y-4">
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
                        <Input
                          id="email"
                          type="email"
                          value={formData.email}
                          onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                          disabled={!!user}
                          required
                        />
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

                      <Separator className="my-4" />

                      <div className="flex items-center justify-between p-4 border rounded-lg">
                        <div className="space-y-0.5">
                          <Label htmlFor="isActive" className="text-base">アカウント有効化</Label>
                          <p className="text-sm text-muted-foreground">
                            無効にするとユーザーはログインできなくなります
                          </p>
                        </div>
                        <Switch
                          id="isActive"
                          checked={formData.isActive}
                          onCheckedChange={(checked) => setFormData(prev => ({ ...prev, isActive: checked }))}
                        />
                      </div>
                    </div>
                  )}

                  {activeSection === 'roles' && (
                    <div className="space-y-4">
                      {[
                        { id: 'ADMIN', label: 'システム管理者 (ADMIN)', desc: 'システム全体の完全な管理権限を持ちます' },
                        { id: 'TENANT_ADMIN', label: 'テナント管理者 (TENANT_ADMIN)', desc: '所属するテナントの管理権限を持ちます' },
                        { id: 'USER', label: '一般ユーザー (USER)', desc: '一般的な機能の利用権限のみを持ちます' },
                      ].map((role) => (
                        <div
                          key={role.id}
                          className={cn(
                            "flex flex-row items-center justify-between rounded-lg border p-4 transition-all",
                            roleCheckboxes[role.id as keyof typeof roleCheckboxes]
                              ? "border-primary bg-primary/5"
                              : "hover:bg-muted/50"
                          )}
                        >
                          <div className="space-y-0.5 flex-1">
                            <Label htmlFor={`role-${role.id}`} className="text-base cursor-pointer">
                              {role.label}
                            </Label>
                            <p className="text-sm text-muted-foreground">
                              {role.desc}
                            </p>
                          </div>
                          <Switch
                            id={`role-${role.id}`}
                            checked={roleCheckboxes[role.id as keyof typeof roleCheckboxes]}
                            onCheckedChange={() => handleRoleToggle(role.id as keyof typeof roleCheckboxes)}
                          />
                        </div>
                      ))}
                    </div>
                  )}
                </form>

                {/* Tenant Logic is separate from main form/submit */}
                {activeSection === 'tenants' && (
                  <div className="space-y-4">
                    {!user ? (
                      <div className="text-center py-8 text-muted-foreground border rounded-lg border-dashed">
                        ユーザーを作成後にテナントを割り当てることができます
                      </div>
                    ) : (
                      <>
                        <div className="flex justify-end">
                          <Button onClick={() => setIsTenantSelectorOpen(true)} size="sm">
                            <Plus className="mr-2 h-4 w-4" />
                            テナントを追加
                          </Button>
                        </div>

                        <div className="border rounded-md divide-y">
                          {tenantAssignments.size === 0 ? (
                            <div className="p-8 text-center text-muted-foreground">
                              所属しているテナントはありません
                            </div>
                          ) : (
                            Array.from(tenantAssignments.entries()).map(([tenantId, data]) => (
                              <div key={tenantId} className="p-4 flex flex-col sm:flex-row sm:items-center gap-4">
                                <div className="flex-1 min-w-0">
                                  <div className="font-medium truncate flex items-center gap-2">
                                    {data.tenantName}
                                    {data.isDefault && <Badge variant="outline" className="text-xs">Default</Badge>}
                                  </div>
                                  {data.domain && <div className="text-xs text-muted-foreground truncate">{data.domain}</div>}
                                </div>
                                
                                <div className="flex items-center gap-3">
                                  <Select 
                                    value={data.roleInTenant} 
                                    onValueChange={(v) => handleTenantRoleChange(tenantId, v)}
                                  >
                                    <SelectTrigger className="w-32">
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="USER">USER</SelectItem>
                                      <SelectItem value="ADMIN">ADMIN</SelectItem>
                                      <SelectItem value="MEMBER">MEMBER</SelectItem>
                                    </SelectContent>
                                  </Select>

                                  <Button 
                                    onClick={() => handleTenantDefaultChange(tenantId)}
                                    disabled={data.isDefault}
                                    variant="ghost"
                                    size="sm"
                                    className={cn("text-xs", data.isDefault && "text-primary font-medium")}
                                  >
                                    デフォルト
                                  </Button>

                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="text-muted-foreground hover:text-destructive"
                                    onClick={() => handleRemoveTenant(tenantId)}
                                  >
                                    <Trash2 className="h-4 w-4" />
                                  </Button>
                                </div>
                              </div>
                            ))
                          )}
                        </div>
                        
                        {tenantAssignments.size > 0 && (
                          <div className="flex justify-end pt-4">
                            <Button 
                              onClick={handleSaveTenants} 
                              disabled={isSavingTenants}
                              className="min-w-[120px]"
                            >
                              {isSavingTenants ? '保存中...' : '変更を保存'}
                            </Button>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                )}

                {activeSection === 'account' && user && (
                  <Card>
                    <CardHeader>
                      <CardTitle className="text-base">アカウント監査情報</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4 text-sm">
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div>
                          <p className="text-muted-foreground mb-1">ユーザーID</p>
                          <p className="font-mono">{user.userId}</p>
                        </div>
                        <div>
                          <p className="text-muted-foreground mb-1">作成日時</p>
                          <p>{new Date(user.createdAt).toLocaleString('ja-JP')}</p>
                        </div>
                        <div>
                          <p className="text-muted-foreground mb-1">最終ログイン</p>
                          <p>{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('ja-JP') : '未ログイン'}</p>
                        </div>
                        <div>
                          <p className="text-muted-foreground mb-1">メール認証状態</p>
                          <Badge variant={user.emailVerified ? 'success' : 'outline'}>
                            {user.emailVerified ? '認証済み' : '未認証'}
                          </Badge>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )}
              </div>
            </div>

            {/* Footer */}
            {activeSection !== 'tenants' && (
              <div className="p-4 border-t bg-background flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
                  キャンセル
                </Button>
                <Button 
                  type="submit" 
                  form="user-form" 
                  disabled={isLoading}
                >
                  {isLoading ? '保存中...' : (user ? '更新' : '作成')}
                </Button>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      <TenantSelectorDialog
        open={isTenantSelectorOpen}
        onClose={() => setIsTenantSelectorOpen(false)}
        onSelect={handleAddTenants}
        excludedTenantIds={Array.from(tenantAssignments.keys())}
      />

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
