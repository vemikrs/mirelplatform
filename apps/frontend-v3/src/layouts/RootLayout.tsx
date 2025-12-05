import { useMemo, useState, useEffect } from 'react';
import { Link, Outlet, useLoaderData } from 'react-router-dom';
import { Button, Toaster, Dialog, DialogContent, DialogTrigger } from '@mirel/ui';
import type { NavigationConfig, NavigationLink } from '@/app/navigation.schema';
import { Menu } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useAuthStore } from '@/stores/authStore';
import { SideNavigation } from '@/components/layouts/SideNavigation';
import { getMenuTree, adaptMenuToNavigationLink } from '@/lib/api/menu';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';


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

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const primaryLinks = useMemo(() => {
    if (dynamicMenu && dynamicMenu.length > 0) {
      return dynamicMenu.map(adaptMenuToNavigationLink);
    }
    return initialNavigation.primary;
  }, [dynamicMenu, initialNavigation.primary]);

  const helpAction = initialNavigation.globalActions.find(a => a.type === 'help');

  return (
    <div className="flex min-h-screen flex-col bg-surface text-foreground">
      <header className="sticky top-0 z-40 border-b border-outline/20 bg-surface/70 backdrop-blur-xl md:hidden">
        <div className="flex h-16 items-center justify-between gap-4 px-4 md:h-20 md:gap-6 md:px-6">
          <div className="flex flex-1 items-center gap-6">
            {/* Mobile menu button */}
            <Dialog open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
              <DialogTrigger asChild>
                <Button variant="ghost" size="icon" className="md:hidden">
                  <Menu className="size-5" />
                </Button>
              </DialogTrigger>
              <DialogContent className="p-0 max-w-[288px] h-full top-0 left-0 translate-x-0 translate-y-0 data-[state=closed]:-translate-x-full">
                <SideNavigation 
                  items={primaryLinks}
                  brand={initialNavigation.brand}
                  helpAction={helpAction}
                  className="h-full border-0"
                />
              </DialogContent>
            </Dialog>
            
            {/* Brand - show on mobile only in header */}
            <Link to="/home" className="group flex items-center gap-3 text-left md:hidden">
              <div className="rounded-full bg-primary/10 px-3 py-1 text-sm font-semibold text-primary">
                {initialNavigation.brand.shortName ?? initialNavigation.brand.name}
              </div>
            </Link>
          </div>
          <div className="hidden items-center gap-2 md:flex">
            {/* Desktop Global Actions moved to sidebar */}
          </div>
        </div>
      </header>

      <div className="flex flex-1 items-start">
        <SideNavigation 
          items={primaryLinks}
          brand={initialNavigation.brand}
          helpAction={helpAction}
          className="hidden md:flex sticky top-0 h-screen overflow-y-auto shrink-0" 
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
