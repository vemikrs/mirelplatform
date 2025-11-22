import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { useTheme } from '@/lib/hooks/useTheme';
import { Button, Badge } from '@mirel/ui';
import { User, Settings, LogOut, ChevronDown, SunMedium, MoonStar, Eye, EyeOff } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getUserLicenses, type LicenseInfo } from '@/lib/api/userProfile';

const QUICK_LINKS_STORAGE_KEY = 'mirel-quicklinks-visible';

type LicenseTier = 'FREE' | 'TRIAL' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';

/**
 * ユーザーメニューコンポーネント
 */
export function UserMenu() {
  const { user, logout, tokens } = useAuth();
  const { themeMode, setTheme } = useTheme();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const [quickLinksVisible, setQuickLinksVisible] = useState(() => {
    if (typeof window === 'undefined') return true;
    const stored = window.localStorage.getItem(QUICK_LINKS_STORAGE_KEY);
    return stored === null ? true : stored === 'true';
  });

  // 外側クリックで閉じる
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  const toggleQuickLinks = () => {
    const newValue = !quickLinksVisible;
    setQuickLinksVisible(newValue);
    window.localStorage.setItem(QUICK_LINKS_STORAGE_KEY, String(newValue));
    // カスタムイベントでRootLayoutに通知
    window.dispatchEvent(new CustomEvent('quicklinks-toggle', { detail: { visible: newValue } }));
  };

  // Fetch user's licenses from API
  const { data: licenses = [] } = useQuery<LicenseInfo[]>({
    queryKey: ['userLicenses'],
    queryFn: async () => {
      if (!tokens?.accessToken) return [];
      return getUserLicenses(tokens.accessToken);
    },
    enabled: !!tokens?.accessToken,
  });

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
    <div className="relative" ref={menuRef}>
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
        <div className="absolute right-0 mt-2 w-64 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-200 dark:border-gray-700 z-50">
          <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700">
            <p className="text-sm font-medium">{user.displayName}</p>
            <p className="text-xs text-muted-foreground">@{user.username}</p>
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
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              <User className="w-4 h-4" />
              プロフィール設定
            </button>
            
            <button
              onClick={() => {
                setIsOpen(false);
                navigate('/settings/security');
              }}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              <Settings className="w-4 h-4" />
              セキュリティ設定
            </button>
          </div>

          <div className="py-1 border-t border-gray-200 dark:border-gray-700">
            <div className="px-4 py-2">
              <p className="text-xs font-semibold text-gray-500 dark:text-gray-400 mb-2">テーマ</p>
              <div className="space-y-1">
                <button
                  onClick={() => setTheme('light')}
                  className={`w-full flex items-center gap-2 px-3 py-2 text-sm rounded transition-colors ${
                    themeMode === 'light'
                      ? 'bg-blue-50 text-blue-600 dark:bg-blue-950 dark:text-blue-400 font-medium'
                      : 'hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`}
                >
                  <SunMedium className="w-4 h-4" />
                  ライトモード
                </button>
                <button
                  onClick={() => setTheme('dark')}
                  className={`w-full flex items-center gap-2 px-3 py-2 text-sm rounded transition-colors ${
                    themeMode === 'dark'
                      ? 'bg-blue-50 text-blue-600 dark:bg-blue-950 dark:text-blue-400 font-medium'
                      : 'hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`}
                >
                  <MoonStar className="w-4 h-4" />
                  ダークモード
                </button>
                <button
                  onClick={() => setTheme('system')}
                  className={`w-full flex items-center gap-2 px-3 py-2 text-sm rounded transition-colors ${
                    themeMode === 'system'
                      ? 'bg-blue-50 text-blue-600 dark:bg-blue-950 dark:text-blue-400 font-medium'
                      : 'hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`}
                >
                  <Settings className="w-4 h-4" />
                  システムに従う
                </button>
              </div>
            </div>
          </div>

          <div className="py-1 border-t border-gray-200 dark:border-gray-700">
            <button
              onClick={toggleQuickLinks}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              {quickLinksVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              Quick Links {quickLinksVisible ? '非表示' : '表示'}
            </button>
          </div>

          <div className="py-1 border-t border-gray-200 dark:border-gray-700">
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
