import { useState, useCallback } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
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
  type LucideIcon 
} from 'lucide-react';
import { cn } from '@mirel/ui';
import type { NavigationLink } from '@/app/navigation.schema';

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

interface SideNavigationProps {
  items: NavigationLink[];
  className?: string;
}

export function SideNavigation({ items, className }: SideNavigationProps) {
  const location = useLocation();
  
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
            (child.children && child.children.some(grandchild => 
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
    <nav className={cn("w-64 bg-surface-subtle border-r border-outline/20 flex flex-col py-4", className)}>
      <div className="px-3 space-y-1">
        {items.map((item) => (
          <NavItem 
            key={item.id} 
            item={item} 
            expandedItems={expandedItems}
            onToggle={toggleItem}
          />
        ))}
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
  const paddingLeft = depth > 0 ? `${depth * 0.75 + 0.75}rem` : '0.75rem';

  if (hasChildren) {
    return (
      <div className="space-y-0.5">
        <button
          onClick={() => onToggle(item.id)}
          className={cn(
            "w-full flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md transition-all duration-200",
            "text-muted-foreground hover:bg-surface-raised hover:text-foreground",
            isOpen && "bg-surface-raised/50"
          )}
          style={{ paddingLeft }}
        >
          {isOpen ? (
            <ChevronDown className="size-4 opacity-60 shrink-0" />
          ) : (
            <ChevronRight className="size-4 opacity-60 shrink-0" />
          )}
          {Icon && <Icon className="size-4 shrink-0" />}
          <span className="flex-1 text-left truncate">{item.label}</span>
        </button>
        
        {isOpen && (
          <div className="space-y-0.5 ml-2 border-l border-outline/20 pl-1">
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
          "flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md transition-all duration-200",
          isActive
            ? "bg-primary/10 text-primary"
            : "text-muted-foreground hover:bg-surface-raised hover:text-foreground"
        )
      }
      style={{ paddingLeft }}
    >
      {depth > 0 && <span className="w-4" />}
      {Icon && <Icon className="size-4 shrink-0" />}
      <span className="truncate">{item.label}</span>
    </NavLink>
  );
}
