import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { useTheme, type ThemeMode } from '@/lib/hooks/useTheme';
import { 
  Button, 
  Badge, 
  Avatar,
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuGroup,
  DropdownMenuSub,
  DropdownMenuSubTrigger,
  DropdownMenuSubContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuPortal,
  DropdownMenuLabel,
  Tooltip,
  TooltipTrigger,
  TooltipContent,
  cn
} from '@mirel/ui';
import { User, Settings, LogOut, SunMedium, MoonStar, Eye, EyeOff, Building2, ChevronDown, ChevronRight, ChevronsUpDown } from 'lucide-react';

const QUICK_LINKS_STORAGE_KEY = 'mirel-quicklinks-visible';

type LicenseTier = 'FREE' | 'TRIAL' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';

interface SidebarUserMenuProps {
  isExpanded: boolean;
}

/**
 * サイドバー用ユーザーメニューコンポーネント
 * 
 * 修正:
 * - 展開時 (isExpanded=true): インラインの単純な開閉動作に変更 (オーバーレイ廃止)
 * - 折りたたみ時 (isExpanded=false): DropdownMenu (Popover) を維持
 * - 固定高さ (h-[68px]) を削除
 */
export function SidebarUserMenu({ isExpanded }: SidebarUserMenuProps) {
  const { user, currentTenant, switchTenant, tenants, licenses } = useAuth();
  const { themeMode, setTheme } = useTheme();
  const navigate = useNavigate();
  const [quickLinksVisible, setQuickLinksVisible] = useState(() => {
    if (typeof window === 'undefined') return true;
    const stored = window.localStorage.getItem(QUICK_LINKS_STORAGE_KEY);
    return stored === null ? true : stored === 'true';
  });

  // Inline menu state
  const [isOpen, setIsOpen] = useState(false);
  const [isWorkspaceOpen, setIsWorkspaceOpen] = useState(false);
  const [isThemeOpen, setIsThemeOpen] = useState(false);

  const toggleQuickLinks = (e?: React.MouseEvent) => {
    e?.stopPropagation();
    const newValue = !quickLinksVisible;
    setQuickLinksVisible(newValue);
    window.localStorage.setItem(QUICK_LINKS_STORAGE_KEY, String(newValue));
    window.dispatchEvent(new CustomEvent('quicklinks-toggle', { detail: { visible: newValue } }));
  };

  const handleLogout = () => {
    navigate('/logout');
  };

  const handleTenantSwitch = async (tenantId: string) => {
    if (tenantId === currentTenant?.tenantId) return;
    try {
      await switchTenant(tenantId);
    } catch (error) {
      console.error('Failed to switch tenant:', error);
    }
  };

  if (!user) {
    return null;
  }

  const currentLicense = licenses.find((license) => license.applicationId === 'promarker');
  const currentTier: LicenseTier = currentLicense?.tier || 'FREE';

  const tierColors: Record<LicenseTier, string> = {
    FREE: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200',
    TRIAL: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
    BASIC: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
    PROFESSIONAL: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
    ENTERPRISE: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  };

  const tierLabels: Record<LicenseTier, string> = {
    FREE: 'Free',
    TRIAL: 'Trial',
    BASIC: 'Basic',
    PROFESSIONAL: 'Pro',
    ENTERPRISE: 'Enterprise',
  };

  // --- 1. 折りたたみモード (アイコンのみ) ---
  // インライン展開できないため、DropdownMenuを使用
  if (!isExpanded) {
    return (
      <DropdownMenu>
        <Tooltip>
          <TooltipTrigger asChild>
            <DropdownMenuTrigger asChild>
              <Button 
                variant="ghost" 
                size="icon"
                className="w-full h-10 flex items-center justify-center"
              >
                <Avatar 
                  src={user.avatarUrl}
                  alt={user.displayName || user.email}
                  fallback={user.displayName?.charAt(0)?.toUpperCase() || user.email?.charAt(0)?.toUpperCase() || 'U'}
                  size="sm"
                />
              </Button>
            </DropdownMenuTrigger>
          </TooltipTrigger>
          <TooltipContent side="right">
            {user.displayName || user.email}
          </TooltipContent>
        </Tooltip>
        
        <DropdownMenuContent className="w-64" side="right" align="end">
          <UserMenuContent 
            user={user}
            currentTenant={currentTenant}
            tenants={tenants}
            currentTier={currentTier}
            tierColors={tierColors}
            tierLabels={tierLabels}
            themeMode={themeMode}
            setTheme={setTheme}
            quickLinksVisible={quickLinksVisible}
            toggleQuickLinks={() => toggleQuickLinks()}
            handleTenantSwitch={handleTenantSwitch}
            handleLogout={handleLogout}
            navigate={navigate}
          />
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  // --- 2. 展開モード (フル表示) ---
  // DropdownMenuを廃止し、単純な開閉ロジックを使用したインライン展開に変更
  return (
    <div className="w-full h-full">
      <Button 
        variant="ghost" 
        className="w-full h-full flex items-center gap-2 px-2 py-2 justify-between hover:bg-surface-raised group"
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="flex items-center gap-2 min-w-0 flex-1 text-left">
          <Avatar 
            src={user.avatarUrl}
            alt={user.displayName || user.email}
            fallback={user.displayName?.charAt(0)?.toUpperCase() || user.email?.charAt(0)?.toUpperCase() || 'U'}
            size="sm"
          />
          <div className="flex flex-col min-w-0">
            <span className="text-sm font-medium leading-tight truncate">
              {user.displayName || user.email}
            </span>
            {currentTenant && (
              <span className="text-xs text-muted-foreground mt-0.5 truncate">
                {currentTenant.displayName}
              </span>
            )}
          </div>
        </div>
        <ChevronsUpDown className="h-4 w-4 text-muted-foreground opacity-50 group-hover:opacity-100" />
      </Button>
      
      {isOpen && (
        <div className="space-y-1 px-1 py-1 animate-in fade-in slide-in-from-top-1 duration-200">
          {/* ユーザー詳細・プラン情報 */}
          <div className="p-2 bg-surface-raised/50 rounded-md mb-2">
            <div className="flex items-center gap-2 mb-2">
              <div className="overflow-hidden">
                <p className="text-xs font-medium text-muted-foreground">@{user.username}</p>
                <p className="text-xs text-muted-foreground truncate">{user.email}</p>
              </div>
            </div>
            <Badge className={cn("w-full justify-center text-[10px] py-0 h-5", tierColors[currentTier])}>
              {tierLabels[currentTier]} プラン
            </Badge>
          </div>

          {/* ワークスペース切替 (サブアコーディオン) */}
          <div className="space-y-0.5">
            <Button 
              variant="ghost" 
              size="sm" 
              className="w-full justify-start text-xs font-normal h-8 px-2"
              onClick={() => setIsWorkspaceOpen(!isWorkspaceOpen)}
            >
              <Building2 className="mr-2 h-3.5 w-3.5" />
              <span className="flex-1 text-left">ワークスペース切替</span>
              {isWorkspaceOpen ? <ChevronDown className="h-3 w-3 opacity-50" /> : <ChevronRight className="h-3 w-3 opacity-50" />}
            </Button>
            
            {isWorkspaceOpen && (
              <div className="pl-4 space-y-0.5 border-l border-outline/20 ml-3.5 my-1">
                {tenants.map((tenant) => (
                  <Button
                    key={tenant.tenantId}
                    variant="ghost"
                    size="sm"
                    className={cn(
                      "w-full justify-start text-xs h-7 px-2",
                      currentTenant?.tenantId === tenant.tenantId && "bg-primary/5 text-primary"
                    )}
                    onClick={() => handleTenantSwitch(tenant.tenantId)}
                  >
                    <span className="truncate">{tenant.displayName}</span>
                    {currentTenant?.tenantId === tenant.tenantId && <div className="ml-auto w-1.5 h-1.5 rounded-full bg-primary" />}
                  </Button>
                ))}
              </div>
            )}
          </div>

          {/* プロフィール設定 */}
          <Button 
            variant="ghost" 
            size="sm" 
            className="w-full justify-start text-xs font-normal h-8 px-2"
            onClick={() => navigate('/settings/profile')}
          >
            <User className="mr-2 h-3.5 w-3.5" />
            <span>プロフィール設定</span>
          </Button>

          {/* セキュリティ設定 */}
          <Button 
            variant="ghost" 
            size="sm" 
            className="w-full justify-start text-xs font-normal h-8 px-2"
            onClick={() => navigate('/settings/profile?tab=security')}
          >
            <Settings className="mr-2 h-3.5 w-3.5" />
            <span>セキュリティ設定</span>
          </Button>

          <div className="h-px bg-outline/20 my-1 mx-2" />

          {/* テーマ設定 (サブアコーディオン) */}
          <div className="space-y-0.5">
             <Button 
              variant="ghost" 
              size="sm" 
              className="w-full justify-start text-xs font-normal h-8 px-2"
              onClick={() => setIsThemeOpen(!isThemeOpen)}
            >
              {themeMode === 'light' ? <SunMedium className="mr-2 h-3.5 w-3.5" /> : 
               themeMode === 'dark' ? <MoonStar className="mr-2 h-3.5 w-3.5" /> : 
               <Settings className="mr-2 h-3.5 w-3.5" />}
              <span className="flex-1 text-left">テーマ設定</span>
              {isThemeOpen ? <ChevronDown className="h-3 w-3 opacity-50" /> : <ChevronRight className="h-3 w-3 opacity-50" />}
            </Button>
            
            {isThemeOpen && (
              <div className="pl-4 space-y-0.5 border-l border-outline/20 ml-3.5 my-1">
                <Button
                  variant="ghost"
                  size="sm"
                  className={cn("w-full justify-start text-xs h-7 px-2", themeMode === 'light' && "text-primary")}
                  onClick={() => setTheme('light')}
                >
                  <SunMedium className="mr-2 h-3 w-3" />
                  <span>ライトモード</span>
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className={cn("w-full justify-start text-xs h-7 px-2", themeMode === 'dark' && "text-primary")}
                  onClick={() => setTheme('dark')}
                >
                  <MoonStar className="mr-2 h-3 w-3" />
                  <span>ダークモード</span>
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className={cn("w-full justify-start text-xs h-7 px-2", themeMode === 'system' && "text-primary")}
                  onClick={() => setTheme('system')}
                >
                  <Settings className="mr-2 h-3 w-3" />
                  <span>システムに従う</span>
                </Button>
              </div>
            )}
          </div>

          {/* Quick Links Toggle */}
          <Button 
            variant="ghost" 
            size="sm" 
            className="w-full justify-start text-xs font-normal h-8 px-2"
            onClick={toggleQuickLinks}
          >
            {quickLinksVisible ? <EyeOff className="mr-2 h-3.5 w-3.5" /> : <Eye className="mr-2 h-3.5 w-3.5" />}
            <span>Quick Links {quickLinksVisible ? '非表示' : '表示'}</span>
          </Button>

          <div className="h-px bg-outline/20 my-1 mx-2" />

          {/* ログアウト */}
          <Button 
            variant="ghost" 
            size="sm" 
            className="w-full justify-start text-xs font-normal h-8 px-2 text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/20"
            onClick={handleLogout}
          >
            <LogOut className="mr-2 h-3.5 w-3.5" />
            <span>ログアウト</span>
          </Button>
        </div>
      )}
    </div>
  );
}

// Shared menu content component (for Collapsed/Dropdown mode)
interface UserMenuContentProps {
  user: { displayName?: string; email?: string; username?: string; avatarUrl?: string };
  currentTenant: { tenantId: string; displayName: string } | null;
  tenants: Array<{ tenantId: string; displayName: string }>;
  currentTier: LicenseTier;
  tierColors: Record<LicenseTier, string>;
  tierLabels: Record<LicenseTier, string>;
  themeMode: ThemeMode;
  setTheme: (mode: ThemeMode) => void;
  quickLinksVisible: boolean;
  toggleQuickLinks: () => void;
  handleTenantSwitch: (tenantId: string) => void;
  handleLogout: () => void;
  navigate: (path: string) => void;
}

function UserMenuContent({
  user,
  currentTenant,
  tenants,
  currentTier,
  tierColors,
  tierLabels,
  themeMode,
  setTheme,
  quickLinksVisible,
  toggleQuickLinks,
  handleTenantSwitch,
  handleLogout,
  navigate,
}: UserMenuContentProps) {
  return (
    <>
      <div className="px-2 py-1.5">
        <div className="flex items-center gap-3 mb-2">
          <Avatar 
            src={user.avatarUrl}
            alt={user.displayName || user.email}
            fallback={user.displayName?.charAt(0)?.toUpperCase() || user.email?.charAt(0)?.toUpperCase() || 'U'}
            size="md"
          />
          <div className="overflow-hidden">
            <p className="text-sm font-medium truncate">{user.displayName}</p>
            <p className="text-xs text-muted-foreground truncate">@{user.username}</p>
          </div>
        </div>
        <p className="text-xs text-muted-foreground truncate px-1 mb-2">{user.email}</p>
        <Badge className={cn("w-full justify-center", tierColors[currentTier])}>
          {tierLabels[currentTier]} プラン
        </Badge>
      </div>

      <DropdownMenuSeparator />

      <DropdownMenuGroup>
        <DropdownMenuLabel>ワークスペース</DropdownMenuLabel>
        <DropdownMenuSub>
          <DropdownMenuSubTrigger>
            <Building2 className="mr-2 h-4 w-4" />
            <span>ワークスペース切替</span>
          </DropdownMenuSubTrigger>
          <DropdownMenuPortal>
            <DropdownMenuSubContent>
              <DropdownMenuRadioGroup value={currentTenant?.tenantId} onValueChange={handleTenantSwitch}>
                {tenants.map((tenant) => (
                  <DropdownMenuRadioItem key={tenant.tenantId} value={tenant.tenantId}>
                    {tenant.displayName}
                  </DropdownMenuRadioItem>
                ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuPortal>
        </DropdownMenuSub>
      </DropdownMenuGroup>

      <DropdownMenuSeparator />

      <DropdownMenuGroup>
        <DropdownMenuLabel>アカウント設定</DropdownMenuLabel>
        <DropdownMenuItem onClick={() => navigate('/settings/profile')}>
          <User className="mr-2 h-4 w-4" />
          <span>プロフィール設定</span>
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => navigate('/settings/profile?tab=security')}>
          <Settings className="mr-2 h-4 w-4" />
          <span>セキュリティ設定</span>
        </DropdownMenuItem>
      </DropdownMenuGroup>

      <DropdownMenuSeparator />

      <DropdownMenuGroup>
        <DropdownMenuLabel>表示設定</DropdownMenuLabel>
        <DropdownMenuSub>
          <DropdownMenuSubTrigger>
            {themeMode === 'light' ? <SunMedium className="mr-2 h-4 w-4" /> : 
             themeMode === 'dark' ? <MoonStar className="mr-2 h-4 w-4" /> : 
             <Settings className="mr-2 h-4 w-4" />}
            <span>テーマ設定</span>
          </DropdownMenuSubTrigger>
          <DropdownMenuPortal>
            <DropdownMenuSubContent>
              <DropdownMenuRadioGroup value={themeMode} onValueChange={(val) => setTheme(val as ThemeMode)}>
                <DropdownMenuRadioItem value="light">
                  <SunMedium className="mr-2 h-4 w-4" />
                  ライトモード
                </DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="dark">
                  <MoonStar className="mr-2 h-4 w-4" />
                  ダークモード
                </DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="system">
                  <Settings className="mr-2 h-4 w-4" />
                  システムに従う
                </DropdownMenuRadioItem>
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuPortal>
        </DropdownMenuSub>

        <DropdownMenuItem onClick={toggleQuickLinks}>
          {quickLinksVisible ? <EyeOff className="mr-2 h-4 w-4" /> : <Eye className="mr-2 h-4 w-4" />}
          <span>Quick Links {quickLinksVisible ? '非表示' : '表示'}</span>
        </DropdownMenuItem>
      </DropdownMenuGroup>

      <DropdownMenuSeparator />

      <DropdownMenuItem 
        onClick={handleLogout} 
        className="text-red-600 dark:text-red-400 focus:text-red-600 dark:focus:text-red-400 focus:bg-red-50 dark:focus:bg-red-950/30"
      >
        <LogOut className="mr-2 h-4 w-4" />
        <span>ログアウト</span>
      </DropdownMenuItem>
    </>
  );
}
