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
  DropdownMenuLabel
} from '@mirel/ui';
import { User, Settings, LogOut, ChevronDown, SunMedium, MoonStar, Building2 } from 'lucide-react';


type LicenseTier = 'FREE' | 'TRIAL' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';

/**
 * ユーザーメニューコンポーネント
 */
export function UserMenu() {
  const { user, logout, currentTenant, switchTenant, tenants, licenses } = useAuth();

  const { themeMode, setTheme } = useTheme();
  const navigate = useNavigate();

  const handleLogout = () => {
    console.log('[DEBUG UserMenu] handleLogout called');
    // logout() は window.location.replace('/login') を実行するため、
    // この関数は return しない（ページ遷移する）
    // navigate() は不要
    logout();
    console.log('[DEBUG UserMenu] logout() called (this may not be logged)');
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

  // Find license for promarker application
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

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="flex items-center gap-2 px-2 hover:bg-surface-subtle">
          <Avatar 
            src={undefined}
            alt={user.displayName || user.email}
            fallback={user.displayName?.charAt(0)?.toUpperCase() || user.email?.charAt(0)?.toUpperCase() || 'U'}
            size="sm"
          />
          <div className="hidden sm:flex flex-col items-start text-left">
            <span className="text-sm font-medium leading-none">{user.displayName || user.email}</span>
            {currentTenant && (
              <span className="text-xs text-muted-foreground mt-0.5">{currentTenant.displayName}</span>
            )}
          </div>
          <ChevronDown className="w-4 h-4 text-muted-foreground" />
        </Button>
      </DropdownMenuTrigger>
      
      <DropdownMenuContent className="w-64" align="end">
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
          <Badge className={`w-full justify-center ${tierColors[currentTier]}`}>
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
        </DropdownMenuGroup>

        <DropdownMenuSeparator />

        <DropdownMenuItem onClick={handleLogout} className="text-red-600 dark:text-red-400 focus:text-red-600 dark:focus:text-red-400 focus:bg-red-50 dark:focus:bg-red-950/30">
          <LogOut className="mr-2 h-4 w-4" />
          <span>ログアウト</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
