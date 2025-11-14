import { useEffect, useMemo, useState } from 'react';
import { Link, NavLink, Outlet, useLoaderData } from 'react-router-dom';
import { Badge, Button, Toaster } from '@mirel/ui';
import type { NavigationAction, NavigationConfig, NavigationLink } from '@/app/navigation.schema';
import { Bell, HelpCircle, Menu, SunMedium, MoonStar, UserRound } from 'lucide-react';

const THEME_STORAGE_KEY = 'mirel-theme';

function useThemeToggle() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    if (typeof window === 'undefined') {
      return 'light';
    }
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY) as 'light' | 'dark' | null;
    if (stored) {
      return stored;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    if (typeof document === 'undefined') return;
    document.documentElement.classList.toggle('dark', theme === 'dark');
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  return { theme, toggleTheme };
}

function renderAction(action: NavigationAction, toggleTheme: () => void, currentTheme: 'light' | 'dark') {
  switch (action.type) {
    case 'theme':
      return (
        <Button
          key={action.id}
          variant="ghost"
          size="square"
          aria-label="テーマ切替"
          onClick={toggleTheme}
        >
          {currentTheme === 'dark' ? <SunMedium className="size-5" /> : <MoonStar className="size-5" />}
        </Button>
      );
    case 'notifications':
      return (
        <Button key={action.id} variant="ghost" size="square" aria-label="通知">
          <Bell className="size-5" />
        </Button>
      );
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
    case 'profile': {
      const initials = action.initials || 'ME';
      return (
        <div
          key={action.id}
          className="flex size-9 items-center justify-center rounded-full bg-primary/10 font-semibold text-primary"
          aria-label="プロフィール"
          role="img"
        >
          {initials}
        </div>
      );
    }
    case 'custom':
      return (
        <Button key={action.id} variant="ghost" size="square" aria-label={action.label ?? 'アクション'}>
          <UserRound className="size-5" />
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
  const navigation = useLoaderData() as NavigationConfig;
  const { theme, toggleTheme } = useThemeToggle();

  const primaryLinks = useMemo(() => navigation.primary, [navigation.primary]);

  return (
    <div className="flex min-h-screen flex-col bg-surface text-foreground">
      <header className="sticky top-0 z-40 border-b border-outline/15 bg-surface/60 backdrop-blur-xl">
        <div className="container flex h-20 items-center justify-between gap-6">
          <div className="flex flex-1 items-center gap-6">
            <Link to="/" className="group flex items-center gap-3 text-left">
              <div className="rounded-full bg-primary/10 px-3 py-1 text-sm font-semibold text-primary">
                {navigation.brand.shortName ?? navigation.brand.name}
              </div>
              <div className="space-y-1">
                <p className="text-lg font-semibold leading-none text-foreground group-hover:text-primary transition-colors">
                  {navigation.brand.name}
                </p>
                {navigation.brand.tagline ? (
                  <p className="text-xs text-muted-foreground">{navigation.brand.tagline}</p>
                ) : null}
              </div>
            </Link>
            <nav className="hidden items-center gap-1 md:flex">
              {primaryLinks.map((item: NavigationLink) => (
                <NavLink
                  key={item.id}
                  to={item.path}
                  className={({ isActive }) =>
                    `rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-surface-raised text-foreground shadow-sm'
                        : 'text-muted-foreground hover:bg-surface-subtle hover:text-foreground'
                    }`
                  }
                  aria-label={item.description ?? item.label}
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
          <div className="hidden items-center gap-2 md:flex">
            {navigation.globalActions.map((action) =>
              renderAction(action, toggleTheme, theme)
            )}
          </div>
          <div className="flex items-center gap-2 md:hidden">
            {renderAction({ id: 'theme-inline', type: 'theme' }, toggleTheme, theme)}
            <Button variant="ghost" size="square" aria-label="メニュー">
              <Menu className="size-5" />
            </Button>
          </div>
        </div>
        <nav className="container flex items-center gap-2 overflow-x-auto pb-3 pt-2 md:hidden">
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

      {navigation.quickLinks.length > 0 ? (
        <div className="border-b border-outline/40 bg-surface-subtle/70">
          <div className="container flex flex-wrap items-center gap-2 py-3">
            <span className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Quick Links
            </span>
            {navigation.quickLinks.map((link: NavigationLink) => (
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

      <main className="flex-1 bg-surface-subtle/40 py-10">
        <div className="container">
          <Outlet />
        </div>
      </main>

      <footer className="mt-auto border-t border-outline/40 bg-surface-subtle/60">
        <div className="container flex flex-col gap-2 py-8 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <div>
            © 2016-2025 mirelplatform. All rights reserved.
          </div>
          <div className="flex items-center gap-3">
            {navigation.secondary.map((link: NavigationLink) => (
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
      <Toaster />
    </div>
  );
}
