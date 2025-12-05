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
  Tooltip,
  TooltipTrigger,
  TooltipContent,
  cn
} from '@mirel/ui';
import { User, Settings, LogOut, SunMedium, MoonStar, Eye, EyeOff, Building2 } from 'lucide-react';

const QUICK_LINKS_STORAGE_KEY = 'mirel-quicklinks-visible';

type LicenseTier = 'FREE' | 'TRIAL' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';

interface SidebarUserMenuProps {
  isCollapsed: boolean;
}

/**
 * サイドバー用ユーザーメニューコンポーネント
 */
export function SidebarUserMenu({ isCollapsed }: SidebarUserMenuProps) {
  const { user, logout, currentTenant, switchTenant, tenants, licenses } = useAuth();
  const { themeMode, setTheme } = useTheme();
  const navigate = useNavigate();
  const [quickLinksVisible, setQuickLinksVisible] = useState(() => {
    if (typeof window === 'undefined') return true;
    const stored = window.localStorage.getItem(QUICK_LINKS_STORAGE_KEY);
    return stored === null ? true : stored === 'true';
  });

  const toggleQuickLinks = () => {
    const newValue = !quickLinksVisible;
    setQuickLinksVisible(newValue);
    window.localStorage.setItem(QUICK_LINKS_STORAGE_KEY, String(newValue));
    window.dispatchEvent(new CustomEvent('quicklinks-toggle', { detail: { visible: newValue } }));
  };

  const handleLogout = () => {
    logout();
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

  // Collapsed mode - show only avatar with tooltip
  if (isCollapsed) {
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
                  src={undefined}
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
            toggleQuickLinks={toggleQuickLinks}
            handleTenantSwitch={handleTenantSwitch}
            handleLogout={handleLogout}
            navigate={navigate}
          />
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  // Expanded mode - show full user info
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button 
          variant="ghost" 
          className="w-full flex items-center gap-2 px-2 py-2 h-auto justify-start hover:bg-surface-raised"
        >
          <Avatar 
            src={undefined}
            alt={user.displayName || user.email}
            fallback={user.displayName?.charAt(0)?.toUpperCase() || user.email?.charAt(0)?.toUpperCase() || 'U'}
            size="sm"
          />
          <div className="flex-1 flex flex-col items-start text-left min-w-0">
            <span className="text-sm font-medium leading-none truncate w-full">
              {user.displayName || user.email}
            </span>
            {currentTenant && (
              <span className="text-xs text-muted-foreground mt-1 truncate w-full">
                {currentTenant.displayName}
              </span>
            )}
          </div>
        </Button>
      </DropdownMenuTrigger>
      
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
          toggleQuickLinks={toggleQuickLinks}
          handleTenantSwitch={handleTenantSwitch}
          handleLogout={handleLogout}
          navigate={navigate}
        />
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

// Shared menu content component
interface UserMenuContentProps {
  user: { displayName?: string; email?: string; username?: string };
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
            src={undefined}
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
        <DropdownMenuItem onClick={() => navigate('/settings/profile')}>
          <User className="mr-2 h-4 w-4" />
          <span>プロフィール設定</span>
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => navigate('/settings/security')}>
          <Settings className="mr-2 h-4 w-4" />
          <span>セキュリティ設定</span>
        </DropdownMenuItem>
      </DropdownMenuGroup>

      <DropdownMenuSeparator />

      <DropdownMenuGroup>
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
