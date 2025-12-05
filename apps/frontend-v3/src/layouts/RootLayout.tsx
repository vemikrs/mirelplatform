import { useMemo, useState, useEffect } from 'react';
import { Link, NavLink, Outlet, useLoaderData } from 'react-router-dom';
import { Badge, Button, Toaster } from '@mirel/ui';
import type { NavigationAction, NavigationConfig, NavigationLink } from '@/app/navigation.schema';
import { Bell, HelpCircle } from 'lucide-react';
import { UserMenu } from '@/components/header/UserMenu';
import { useAuth } from '@/hooks/useAuth';
import { useAuthStore } from '@/stores/authStore';
import { SideNavigation } from '@/components/layouts/SideNavigation';
import { GlobalSearch } from '@/components/header/GlobalSearch';
import { getMenuTree, adaptMenuToNavigationLink } from '@/lib/api/menu';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

import { getUnreadCount } from '@/lib/api/announcement';

const QUICK_LINKS_STORAGE_KEY = 'mirel-quicklinks-visible';

import { Popover, PopoverContent, PopoverTrigger } from '@mirel/ui';
import { NotificationList } from '@/features/home/components/NotificationList';

function NotificationPopover() {
  const { data } = useQuery({
    queryKey: ['unread-count'],
    queryFn: getUnreadCount,
    refetchInterval: 60000, // 1 minute
  });

  const count = data?.count || 0;

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="square" aria-label="通知" className="relative">
          <Bell className="size-5" />
          {count > 0 && (
            <span className="absolute top-2 right-2 size-2 rounded-full bg-red-500 ring-2 ring-background flex items-center justify-center text-[8px] font-bold text-white">
              {count > 9 ? '9+' : count}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[380px] p-0" align="end">
        <div className="p-4 border-b border-border">
          <h4 className="font-semibold leading-none">お知らせ</h4>
        </div>
        <div className="p-2">
          <NotificationList variant="popover" />
        </div>
      </PopoverContent>
    </Popover>
  );
}

function renderAction(action: NavigationAction) {
  switch (action.type) {
    case 'notifications':
      return <NotificationPopover key={action.id} />;
    case 'help':
      return (
        <Button
          key={action.id}
          variant="ghost"
          size="square"
          aria-label="ヘルプセンター"
          asChild={Boolean(action.path)}
        >
          {action.path ? (
            <Link to={action.path}>{<HelpCircle className="size-5" />}</Link>
          ) : (
            <HelpCircle className="size-5" />
          )}
        </Button>
      );
    default:
      return null;
  }
}

/**
 * Root layout component
 * Provides common layout structure for all pages
 */
export function RootLayout() {
  const initialNavigation = useLoaderData() as NavigationConfig;
  const { isAuthenticated } = useAuth();
  const fetchProfile = useAuthStore((state) => state.fetchProfile);

  // Fetch dynamic menu from backend
  const { data: dynamicMenu } = useQuery({
    queryKey: ['menu-tree'],
    queryFn: getMenuTree,
    enabled: isAuthenticated, // Only fetch if authenticated
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  // Sync user profile on mount if authenticated
  useEffect(() => {
    if (isAuthenticated) {
      fetchProfile().catch((error) => {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          // インターセプターで既にログアウト処理が実行されているはず
          // ここでは追加の処理は不要(無限ループ防止)
          return;
        }
        console.error('Failed to fetch profile', error);
      });
    }
  }, [isAuthenticated, fetchProfile]);

  const [quickLinksVisible, setQuickLinksVisible] = useState(() => {
    if (typeof window === 'undefined') return true;
    const stored = window.localStorage.getItem(QUICK_LINKS_STORAGE_KEY);
    return stored === null ? true : stored === 'true';
  });

  useEffect(() => {
    const handleToggle = (event: Event) => {
      const customEvent = event as CustomEvent<{ visible: boolean }>;
      setQuickLinksVisible(customEvent.detail.visible);
    };
    window.addEventListener('quicklinks-toggle', handleToggle);
    return () => window.removeEventListener('quicklinks-toggle', handleToggle);
  }, []);

  const primaryLinks = useMemo(() => {
    if (dynamicMenu && dynamicMenu.length > 0) {
      return dynamicMenu.map(adaptMenuToNavigationLink);
    }
    return initialNavigation.primary;
  }, [dynamicMenu, initialNavigation.primary]);

  return (
    <div className="flex min-h-screen flex-col bg-surface text-foreground">
      <header className="sticky top-0 z-40 border-b border-outline/20 bg-surface/70 backdrop-blur-xl">
        <div className="flex h-16 items-center justify-between gap-4 px-4 md:h-20 md:gap-6 md:px-6">
          <div className="flex flex-1 items-center gap-6">
            {/* Brand moved to sidebar - show on mobile only */}
            <Link to="/home" className="group flex items-center gap-3 text-left md:hidden">
              <div className="rounded-full bg-primary/10 px-3 py-1 text-sm font-semibold text-primary">
                {initialNavigation.brand.shortName ?? initialNavigation.brand.name}
              </div>
              <div className="space-y-1">
                <p className="text-lg font-semibold leading-none text-foreground group-hover:text-primary transition-colors">
                  {initialNavigation.brand.name}
                </p>
                {initialNavigation.brand.tagline ? (
                  <p className="text-xs text-muted-foreground">{initialNavigation.brand.tagline}</p>
                ) : null}
              </div>
            </Link>
            {/* Desktop Nav Removed */}
          </div>
          <div className="hidden items-center gap-2 md:flex">
            <GlobalSearch />
            {initialNavigation.globalActions
              .filter((action) => action.type !== 'theme' && action.type !== 'profile' && action.type !== 'notifications')
              .map((action) => renderAction(action))}
            {/* UserMenu and Notifications moved to sidebar */}
          </div>
          <div className="flex items-center gap-2 md:hidden">
            {isAuthenticated && <UserMenu />}
          </div>
        </div>
        <nav className="flex items-center gap-2 overflow-x-auto px-4 pb-3 pt-2 md:hidden">
          {primaryLinks.map((item: NavigationLink) => (
            <NavLink
              key={item.id}
              to={item.path}
              className={({ isActive }) =>
                `whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium ${
                  isActive
                    ? 'bg-primary text-primary-foreground shadow-sm'
                    : 'bg-surface-subtle text-muted-foreground'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>

      {initialNavigation.quickLinks.length > 0 && quickLinksVisible ? (
        <div className="border-b border-border bg-surface">
          <div className="flex flex-wrap items-center gap-2 px-4 py-3 md:px-6">
            <span className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Quick Links
            </span>
            {initialNavigation.quickLinks.map((link: NavigationLink) => (
              <Badge key={link.id} variant="neutral">
                <Link
                  to={link.path}
                  className="text-xs font-medium text-current"
                  aria-label={link.description ?? link.label}
                >
                  {link.label}
                </Link>
              </Badge>
            ))}
          </div>
        </div>
      ) : null}

      <div className="flex flex-1 items-start">
        <SideNavigation 
          items={primaryLinks}
          brand={initialNavigation.brand}
          className="hidden md:flex sticky top-20 h-[calc(100vh-5rem)] overflow-y-auto shrink-0" 
        />
        
        <div className="flex-1 flex flex-col min-w-0">
          <main className="flex-1 bg-background py-6">
            <div className="px-4 md:px-8">
              <Outlet />
            </div>
          </main>

          <footer className="mt-auto border-t border-outline/40 bg-surface-subtle/60">
            <div className="flex flex-col gap-2 px-4 py-8 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between md:px-6">
              <div>
                © 2016-2025 mirelplatform. All rights reserved.
              </div>
              <div className="flex items-center gap-3">
                {initialNavigation.secondary.map((link: NavigationLink) => (
                  <Link
                    key={link.id}
                    to={link.path}
                    target={link.external ? '_blank' : undefined}
                    rel={link.external ? 'noreferrer' : undefined}
                    className="text-muted-foreground transition-colors hover:text-foreground"
                  >
                    {link.label}
                  </Link>
                ))}
              </div>
            </div>
          </footer>
        </div>
      </div>
      <Toaster />
    </div>
  );
}
