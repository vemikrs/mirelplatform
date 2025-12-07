import { useState, useCallback } from 'react';
import { NavLink, useLocation, Link, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Wand2, 
  Hammer, 
  GitFork, 
  Settings, 
  ChevronRight, 
  ChevronDown,
  Map,
  Building,
  Pin,
  PinOff,
  HelpCircle,
  Bot,
  type LucideIcon 
} from 'lucide-react';
import { cn, Button, Tooltip, TooltipTrigger, TooltipContent } from '@mirel/ui';
import type { NavigationLink, NavigationAction } from '@/app/navigation.schema';
import { SidebarUserMenu } from '@/components/header/SidebarUserMenu';
import { NotificationPopover } from '@/components/header/NotificationPopover';
import { GlobalSearch } from '@/components/header/GlobalSearch';

// Icon mapping
const ICON_MAP: Record<string, LucideIcon> = {
  'layout-dashboard': LayoutDashboard,
  'wand-2': Wand2,
  'hammer': Hammer,
  'git-fork': GitFork,
  'settings': Settings,
  'map': Map,
  'building': Building,
};

// LocalStorage key for sidebar pinned state
const SIDEBAR_PINNED_KEY = 'mirel-sidebar-pinned';

interface SideNavigationProps {
  items: NavigationLink[];
  brand?: {
    name: string;
    shortName?: string;
    tagline?: string;
  };
  helpAction?: NavigationAction;
  className?: string;
}

export function SideNavigation({ items, brand, helpAction, className }: SideNavigationProps) {
  const location = useLocation();
  const navigate = useNavigate();
  
  // Sidebar pinned state (persisted) - デフォルトは false (固定表示オフ)
  const [isPinned, setIsPinned] = useState(() => {
    if (typeof window === 'undefined') return false;
    const stored = window.localStorage.getItem(SIDEBAR_PINNED_KEY);
    return stored === 'true';
  });

  // Temporary expansion state (ボタンクリックで制御)
  const [isTemporaryExpanded, setIsTemporaryExpanded] = useState(false);

  const togglePinned = useCallback(() => {
    setIsPinned(prev => {
      const next = !prev;
      window.localStorage.setItem(SIDEBAR_PINNED_KEY, String(next));
      return next;
    });
  }, []);

  const toggleExpanded = useCallback(() => {
    if (!isPinned) {
      setIsTemporaryExpanded(prev => !prev);
    }
  }, [isPinned]);

  // Determine if sidebar is expanded
  const isExpanded = isPinned || isTemporaryExpanded;
  
  // Track expanded state for each menu item
  const [expandedItems, setExpandedItems] = useState<Set<string>>(() => {
    // Initialize with items that contain the current path
    const expanded = new Set<string>();
    const findExpandedItems = (navItems: NavigationLink[], parentIds: string[] = []) => {
      for (const item of navItems) {
        if (item.children && item.children.length > 0) {
          // Check if any child matches current path
          const hasActiveChild = item.children.some(child => 
            location.pathname === child.path || 
            location.pathname.startsWith(child.path + '/') ||
            (child.children && child.children.some((grandchild: NavigationLink) => 
              location.pathname === grandchild.path || 
              location.pathname.startsWith(grandchild.path + '/')
            ))
          );
          if (hasActiveChild) {
            expanded.add(item.id);
            parentIds.forEach(id => expanded.add(id));
          }
          findExpandedItems(item.children, [...parentIds, item.id]);
        }
      }
    };
    findExpandedItems(items);
    return expanded;
  });

  const toggleItem = useCallback((itemId: string) => {
    setExpandedItems(prev => {
      const next = new Set(prev);
      if (next.has(itemId)) {
        next.delete(itemId);
      } else {
        next.add(itemId);
      }
      return next;
    });
  }, []);

  return (
    <nav 
      className={cn(
        "bg-surface-subtle border-r border-outline/20 flex h-full flex-col transition-all duration-300 ease-in-out",
        isExpanded ? "w-72" : "w-14",
        isTemporaryExpanded && !isPinned && "fixed left-0 top-0 h-screen z-50 shadow-xl",
        className
      )}
      style={{ willChange: 'width', backgroundColor: 'hsl(var(--surface-subtle))' }}
    >
      {/* Brand Section - Fixed at top */}
      {brand && (
        <div className={cn(
          "border-b border-outline/20 py-3",
          isExpanded ? "px-3" : "px-2"
        )}>
          <Link 
            to="/home" 
            className={cn(
              "group flex items-center gap-3 text-left",
              !isExpanded && "justify-center"
            )}
          >
            {!isExpanded ? (
              <Tooltip>
                <TooltipTrigger asChild>
                  <div className="rounded-full bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">
                    {brand.shortName ?? brand.name.substring(0, 2)}
                  </div>
                </TooltipTrigger>
                <TooltipContent side="right">
                  <div>
                    <p className="font-semibold">{brand.name}</p>
                    {brand.tagline && <p className="text-xs text-muted-foreground">{brand.tagline}</p>}
                  </div>
                </TooltipContent>
              </Tooltip>
            ) : (
              <>
                <div className="rounded-full bg-primary/10 px-3 py-1 text-sm font-semibold text-primary shrink-0">
                  {brand.shortName ?? brand.name}
                </div>
                <div className="space-y-0.5 min-w-0">
                  <p className="text-base font-semibold leading-none text-foreground group-hover:text-primary transition-colors truncate">
                    {brand.name}
                  </p>
                  {brand.tagline && (
                    <p className="text-xs text-muted-foreground truncate">{brand.tagline}</p>
                  )}
                </div>
              </>
            )}
          </Link>
        </div>
      )}



      {/* Menu Items - Scrollable (hidden when not expanded) */}
      {isExpanded && (
        <div className="flex-1 overflow-y-auto py-3 px-2 space-y-1">
          {items.map((item) => (
            <NavItem 
              key={item.id} 
              item={item} 
              expandedItems={expandedItems}
              onToggle={toggleItem}
            />
          ))}
        </div>
      )}

      {/* Search Section - Moved here */}
      {isExpanded && (
        <div className="px-3 py-2 border-t border-outline/20 space-y-2">
          <GlobalSearch />
          {/* Mira AI Assistant Button */}
          <Button
            variant="outline"
            className={cn(
              "w-full justify-start gap-2 text-sm font-medium",
              location.pathname === '/mira' && "bg-primary/10 text-primary border-primary/30"
            )}
            onClick={() => navigate('/mira')}
          >
            <Bot className="size-4" />
            <span>Mira (mirel Assistant)</span>
          </Button>
        </div>
      )}

      {/* Bottom Section - Fixed at bottom */}
      <div className={cn(
        "border-t border-outline/20 py-3 mt-auto",
        isExpanded ? "px-3" : "px-2"
      )}>
        {isExpanded ? (
          // Expanded mode UI
          <>
            {/* User Menu + Actions + Notification + Pin/Close Toggle */}
            <div className={cn(
              "flex items-stretch gap-2"
            )}>
              {/* Left: User Menu */}
              <div className="flex-1 min-w-0">
                <SidebarUserMenu isExpanded={isExpanded} />
              </div>


              {/* Help Action */}
              {helpAction && (
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button 
                      variant="ghost" 
                      size="icon"
                      className="size-8"
                      asChild={Boolean('path' in helpAction && helpAction.path)}
                    >
                      {'path' in helpAction && helpAction.path ? (
                        <Link to={helpAction.path} aria-label="ヘルプセンター">
                          <HelpCircle className="size-4" />
                        </Link>
                      ) : (
                        <HelpCircle className="size-4" />
                      )}
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent side="top">
                    ヘルプセンター
                  </TooltipContent>
                </Tooltip>
              )}
              
              {/* Right: Notification (top) + Pin/Close Toggle (bottom) stacked vertically */}
              <div className="flex flex-col justify-between shrink-0 h-[68px]">
                <NotificationPopover isCompact={false} />
                <div className="flex gap-1">
                  {/* Close button (一時展開時のみ) */}
                  {!isPinned && (
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button 
                          variant="ghost" 
                          size="icon"
                          className="size-8"
                          onClick={toggleExpanded}
                          aria-label="サイドバーを閉じる"
                        >
                          <ChevronRight className="size-4 rotate-180" />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent side="right">
                        閉じる
                      </TooltipContent>
                    </Tooltip>
                  )}
                  {/* Pin toggle */}
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button 
                        variant="ghost" 
                        size="icon"
                        className="size-8"
                        onClick={togglePinned}
                        aria-label={isPinned ? "固定を解除" : "サイドバーを固定"}
                      >
                        {isPinned ? <PinOff className="size-4" /> : <Pin className="size-4" />}
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent side="right">
                      {isPinned ? "固定を解除" : "固定表示"}
                    </TooltipContent>
                  </Tooltip>
                </div>
              </div>
            </div>
          </>
        ) : (
          // Collapsed mode UI - シンプルな縦並び
          <div className="flex flex-col items-center gap-2">
            {/* User Menu */}
            <SidebarUserMenu isExpanded={isExpanded} />
            
            {/* Help Action */}
            {helpAction && (
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    className="size-8"
                    asChild={Boolean('path' in helpAction && helpAction.path)}
                  >
                    {'path' in helpAction && helpAction.path ? (
                      <Link to={helpAction.path} aria-label="ヘルプセンター">
                        <HelpCircle className="size-4" />
                      </Link>
                    ) : (
                      <HelpCircle className="size-4" />
                    )}
                  </Button>
                </TooltipTrigger>
                <TooltipContent side="right">
                  ヘルプセンター
                </TooltipContent>
              </Tooltip>
            )}

            {/* Notification */}
            <NotificationPopover isCompact={true} />

            {/* Expand Button */}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-8"
                  onClick={toggleExpanded}
                  aria-label="サイドバーを展開"
                >
                  <ChevronRight className="size-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent side="right">
                サイドバーを展開
              </TooltipContent>
            </Tooltip>
          </div>
        )}
      </div>
    </nav>
  );
}

interface NavItemProps {
  item: NavigationLink;
  depth?: number;
  expandedItems: Set<string>;
  onToggle: (itemId: string) => void;
}

function NavItem({ item, depth = 0, expandedItems, onToggle }: NavItemProps) {
  const Icon = item.icon ? ICON_MAP[item.icon] : null;
  const hasChildren = item.children && item.children.length > 0;
  const isOpen = expandedItems.has(item.id);

  // Indentation for hierarchy
  const paddingLeft = depth > 0 ? `${depth * 0.5 + 0.5}rem` : '0.5rem';

  // Menu with children
  if (hasChildren) {
    return (
      <div className="space-y-1">
        <button
          onClick={() => onToggle(item.id)}
          className={cn(
            "w-full flex items-center gap-2 px-2 py-2 text-sm font-medium rounded-md transition-all duration-200",
            "text-muted-foreground hover:bg-surface-raised hover:text-foreground",
            isOpen && "bg-surface-raised/50"
          )}
          style={{ paddingLeft }}
        >
          {isOpen ? (
            <ChevronDown className="size-3.5 opacity-60 shrink-0" />
          ) : (
            <ChevronRight className="size-3.5 opacity-60 shrink-0" />
          )}
          {Icon && <Icon className="size-4 shrink-0" />}
          <span className="flex-1 text-left truncate">{item.label}</span>
        </button>
        
        {isOpen && (
          <div className="space-y-1 ml-2 border-l border-outline/20 pl-0.5">
            {item.children!.map((child) => (
              <NavItem 
                key={child.id} 
                item={child} 
                depth={depth + 1}
                expandedItems={expandedItems}
                onToggle={onToggle}
              />
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <NavLink
      to={item.path}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-2 px-2 py-2 text-sm font-medium rounded-md transition-all duration-200",
          isActive
            ? "bg-primary/10 text-primary"
            : "text-muted-foreground hover:bg-surface-raised hover:text-foreground"
        )
      }
      style={{ paddingLeft }}
    >
      {depth > 0 && <span className="w-3.5" />}
      {Icon && <Icon className="size-4 shrink-0" />}
      <span className="truncate">{item.label}</span>
    </NavLink>
  );
}
