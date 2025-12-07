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

// LocalStorage key for sidebar expanded state
const SIDEBAR_EXPANDED_KEY = 'mirel-sidebar-expanded';

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
  
  // Single source of truth for expansion state (persisted)
  const [isExpanded, setIsExpanded] = useState(() => {
    if (typeof window === 'undefined') return true;
    const stored = window.localStorage.getItem(SIDEBAR_EXPANDED_KEY);
    // Default to true (expanded) if not set, or restore previous state
    return stored === null ? true : stored === 'true';
  });

  const toggleExpanded = useCallback(() => {
    setIsExpanded(prev => {
      const next = !prev;
      window.localStorage.setItem(SIDEBAR_EXPANDED_KEY, String(next));
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
        "bg-surface-subtle border-r border-outline/20 flex flex-col transition-all duration-300 ease-in-out shrink-0",
        // Sticky position to keep sidebar in view while scrolling
        // self-start prevents the sidebar from being stretched to the full page height by the parent flex container
        // max-h-screen ensures it never exceeds viewport height even if content pushes it
        "sticky top-0 h-screen max-h-screen self-start overflow-hidden",
        isExpanded ? "w-72" : "w-14",
        className
      )}
      style={{ willChange: 'width', backgroundColor: 'hsl(var(--surface-subtle))' }}
    >
      {/* Brand Section - Fixed at top */}
      {brand && (
        <div className={cn(
          "border-b border-outline/20 py-3 shrink-0",
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

      {/* Menu Items - Scrollable */}
      {isExpanded ? (
        <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden py-3 px-2 space-y-1 scrollbar-thin scrollbar-thumb-outline/20 hover:scrollbar-thumb-outline/40">
          {items.map((item) => (
            <NavItem 
              key={item.id} 
              item={item} 
              expandedItems={expandedItems}
              onToggle={toggleItem}
            />
          ))}
        </div>
      ) : (
        /* Collapsed Icon Menu - No scroll needed typically, but safe to allow */
        <div className="flex-1 overflow-y-auto overflow-x-hidden py-3 px-2 space-y-2 scrollbar-none flex flex-col items-center">
          {items.map((item) => (
            <CollapsedNavItem key={item.id} item={item} />
          ))}
        </div>
      )}

      {/* Search Section (Expanded only) */}
      {isExpanded && (
        <div className="px-3 py-2 border-t border-outline/20 space-y-2 shrink-0">
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
        "border-t border-outline/20 py-2 mt-auto shrink-0",
        isExpanded ? "px-3" : "px-2"
      )}>
        {isExpanded ? (
          // Expanded mode UI
          <div className="flex items-stretch gap-2 min-w-0">
            {/* User Menu - Expanded (Inline) - Left side */}
            <div className="flex-1 min-w-0">
              <SidebarUserMenu isExpanded={true} />
            </div>

            {/* Right side controls - Vertical stack */}
            <div className="flex flex-col justify-between shrink-0 gap-1 h-auto">
               <div className="flex flex-col gap-1 items-center">
                 <NotificationPopover isCompact={true} />
               </div>

               <div className="flex flex-col gap-1 items-center mt-auto">
                 {/* Help Action - optional, keeping it small if present */}
                 {helpAction && (
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button 
                          variant="ghost" 
                          size="icon"
                          className="size-7"
                          asChild={Boolean('path' in helpAction && helpAction.path)}
                        >
                          {'path' in helpAction && helpAction.path ? (
                            <Link to={helpAction.path} aria-label="ヘルプセンター">
                              <HelpCircle className="size-3.5" />
                            </Link>
                          ) : (
                            <HelpCircle className="size-3.5" />
                          )}
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent side="left">
                        ヘルプセンター
                      </TooltipContent>
                    </Tooltip>
                  )}

                  {/* Toggle Button */}
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button 
                        variant="ghost" 
                        size="icon"
                        className="size-7 text-muted-foreground hover:text-foreground"
                        onClick={toggleExpanded}
                        aria-label="サイドバーを折りたたむ"
                      >
                        <PanelLeftClose className="size-4" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent side="left">
                      サイドバーを折りたたむ
                    </TooltipContent>
                  </Tooltip>
               </div>
            </div>
          </div>
        ) : (
          // Collapsed mode UI - Simple vertical stack
          <div className="flex flex-col items-center gap-2">
            {/* User Menu - Collapsed (Popover) */}
            <SidebarUserMenu isExpanded={false} />
            
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
                  className="size-8 text-muted-foreground hover:text-foreground"
                  onClick={toggleExpanded}
                  aria-label="サイドバーを展開"
                >
                  <PanelLeftOpen className="size-4" />
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

// Helper icons for toggle
function PanelLeftClose({ className }: { className?: string }) {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      width="24" 
      height="24" 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      strokeWidth="2" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
      className={className}
    >
      <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
      <line x1="9" x2="9" y1="3" y2="21" />
      <path d="m16 15-3-3 3-3" />
    </svg>
  );
}

function PanelLeftOpen({ className }: { className?: string }) {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      width="24" 
      height="24" 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      strokeWidth="2" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
      className={className}
    >
      <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
      <line x1="9" x2="9" y1="3" y2="21" />
      <path d="m14 9 3 3-3 3" />
    </svg>
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

// Collapsed Icon-only NavItem
function CollapsedNavItem({ item }: { item: NavigationLink }) {
  const Icon = item.icon ? ICON_MAP[item.icon] : null;
  if (!Icon) return null;

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <NavLink
          to={item.path}
          className={({ isActive }) =>
            cn(
              "flex items-center justify-center w-8 h-8 rounded-md transition-all duration-200",
              isActive
                ? "bg-primary/10 text-primary"
                : "text-muted-foreground hover:bg-surface-raised hover:text-foreground"
            )
          }
        >
          <Icon className="size-4" />
        </NavLink>
      </TooltipTrigger>
      <TooltipContent side="right">
        {item.label}
      </TooltipContent>
    </Tooltip>
  );
}
