import { useState, useCallback } from 'react';
import { NavLink, useLocation, Link } from 'react-router-dom';
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
  PanelLeftClose,
  PanelLeft,
  type LucideIcon 
} from 'lucide-react';
import { cn, Button, Tooltip, TooltipTrigger, TooltipContent } from '@mirel/ui';
import type { NavigationLink } from '@/app/navigation.schema';
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

// LocalStorage key for sidebar collapsed state
const SIDEBAR_COLLAPSED_KEY = 'mirel-sidebar-collapsed';

interface SideNavigationProps {
  items: NavigationLink[];
  brand?: {
    name: string;
    shortName?: string;
    tagline?: string;
  };
  className?: string;
}

export function SideNavigation({ items, brand, className }: SideNavigationProps) {
  const location = useLocation();
  
  // Sidebar collapsed state (persisted)
  const [isCollapsed, setIsCollapsed] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true';
  });

  const toggleCollapsed = useCallback(() => {
    setIsCollapsed(prev => {
      const next = !prev;
      window.localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(next));
      return next;
    });
  }, []);
  
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
        "bg-surface-subtle border-r border-outline/20 flex flex-col transition-all duration-300",
        isCollapsed ? "w-14" : "w-72",
        className
      )}
    >
      {/* Brand Section - Fixed at top */}
      {brand && (
        <div className={cn(
          "border-b border-outline/20 py-3",
          isCollapsed ? "px-2" : "px-3"
        )}>
          <Link 
            to="/home" 
            className={cn(
              "group flex items-center gap-3 text-left",
              isCollapsed && "justify-center"
            )}
          >
            {isCollapsed ? (
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

      {/* Search Section - Fixed below brand */}
      {!isCollapsed && (
        <div className="px-3 py-2 border-b border-outline/20">
          <GlobalSearch />
        </div>
      )}

      {/* Menu Items - Scrollable (hidden when collapsed) */}
      {!isCollapsed && (
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

      {/* Spacer when collapsed */}
      {isCollapsed && <div className="flex-1" />}

      {/* Bottom Section - Fixed */}
      <div className={cn(
        "border-t border-outline/20 py-3",
        isCollapsed ? "px-2" : "px-3"
      )}>
        {/* User Menu + Notification + Collapse Toggle */}
        <div className={cn(
          "flex",
          isCollapsed ? "flex-col items-center gap-3" : "items-stretch gap-2"
        )}>
          {/* Left: User Menu */}
          <div className="flex-1 min-w-0">
            <SidebarUserMenu isCollapsed={isCollapsed} />
          </div>
          
          {/* Right: Notification (top) + Collapse (bottom) stacked vertically */}
          {!isCollapsed && (
            <div className="flex flex-col justify-between shrink-0 h-[68px]">
              <NotificationPopover isCompact={false} />
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    className="size-8"
                    onClick={toggleCollapsed}
                  >
                    <PanelLeftClose className="size-4" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent side="right">
                  メニューを折りたたむ
                </TooltipContent>
              </Tooltip>
            </div>
          )}
        </div>
        
        {/* Notification + Collapse toggle when collapsed - below avatar */}
        {isCollapsed && (
          <div className="flex flex-col items-center gap-2 mt-2">
            <NotificationPopover isCompact={true} />
            <Tooltip>
              <TooltipTrigger asChild>
                <Button 
                  variant="ghost" 
                  size="icon"
                  className="size-8"
                  onClick={toggleCollapsed}
                >
                  <PanelLeft className="size-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent side="right">
                メニューを展開
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
