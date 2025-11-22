import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { Button, Badge } from '@mirel/ui';
import { User, Settings, LogOut, ChevronDown, SunMedium, MoonStar } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getUserLicenses, type LicenseInfo } from '@/lib/api/userProfile';

const THEME_STORAGE_KEY = 'mirel-theme';

type LicenseTier = 'FREE' | 'TRIAL' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';

/**
 * ユーザーメニューコンポーネント
 */
export function UserMenu() {
  const { user, logout, tokens } = useAuth();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    if (typeof window === 'undefined') return 'light';
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY) as 'light' | 'dark' | null;
    if (stored) return stored;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  // Fetch user's licenses from API
  const { data: licenses = [] } = useQuery<LicenseInfo[]>({
    queryKey: ['userLicenses'],
    queryFn: async () => {
      if (!tokens?.accessToken) return [];
      return getUserLicenses(tokens.accessToken);
    },
    enabled: !!tokens?.accessToken,
  });

  useEffect(() => {
    if (typeof document === 'undefined') return;
    document.documentElement.classList.toggle('dark', theme === 'dark');
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout failed:', error);
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
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-2 rounded-md hover:bg-surface-subtle transition-colors"
      >
        <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary font-medium">
          {user.displayName?.charAt(0).toUpperCase() || 'U'}
        </div>
        <span className="hidden sm:block text-sm font-medium">{user.displayName || user.email}</span>
        <ChevronDown className="w-4 h-4" />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-surface rounded-md shadow-lg border border-outline/20 z-50">
          <div className="px-4 py-3 border-b border-outline/20">
            <p className="text-sm font-medium">{user.displayName}</p>
            <p className="text-xs text-muted-foreground">{user.email}</p>
            <div className="mt-2">
              <Badge className={tierColors[currentTier]}>
                {tierLabels[currentTier]} プラン
              </Badge>
            </div>
          </div>
          
          <div className="py-1">
            <button
              onClick={() => {
                setIsOpen(false);
                navigate('/settings/profile');
              }}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-surface-subtle transition-colors"
            >
              <User className="w-4 h-4" />
              プロフィール設定
            </button>
            
            <button
              onClick={() => {
                setIsOpen(false);
                navigate('/settings/security');
              }}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-surface-subtle transition-colors"
            >
              <Settings className="w-4 h-4" />
              セキュリティ設定
            </button>

            <button
              onClick={() => {
                toggleTheme();
              }}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-surface-subtle transition-colors"
            >
              {theme === 'dark' ? <SunMedium className="w-4 h-4" /> : <MoonStar className="w-4 h-4" />}
              {theme === 'dark' ? 'ライトモード' : 'ダークモード'}
            </button>
          </div>

          <div className="py-1 border-t border-outline/20">
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              ログアウト
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
