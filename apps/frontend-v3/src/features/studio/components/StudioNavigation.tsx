import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { cn } from '@mirel/ui';
import {
  Home,
  Database,
  Layout,
  Workflow,
  Table2,
  Rocket,
  ChevronRight,
  ChevronDown,
  Settings,
  type LucideIcon,
} from 'lucide-react';

/**
 * Navigation item interface
 */
export interface StudioNavItem {
  id: string;
  label: string;
  path: string;
  icon: LucideIcon;
  children?: StudioNavItem[];
  badge?: string | number;
}

/**
 * Default Studio navigation structure
 * Model → Form → Flow → Data → Release の順序で配置
 */
export const defaultStudioNavigation: StudioNavItem[] = [
  {
    id: 'home',
    label: 'Home',
    path: '/apps/studio',
    icon: Home,
  },
  {
    id: 'modeler',
    label: 'Modeler',
    path: '/apps/studio/modeler',
    icon: Database,
    children: [
      {
        id: 'entities',
        label: 'Entities',
        path: '/apps/studio/modeler/models',
        icon: Database,
      },
      {
        id: 'codes',
        label: 'Codes',
        path: '/apps/studio/modeler/codes',
        icon: Settings,
      },
    ],
  },
  {
    id: 'forms',
    label: 'Form Designer',
    path: '/apps/studio',
    icon: Layout,
  },
  {
    id: 'flows',
    label: 'Flow Designer',
    path: '/apps/studio',
    icon: Workflow,
  },
  {
    id: 'data',
    label: 'Data Browser',
    path: '/apps/studio/modeler/records',
    icon: Table2,
  },
  {
    id: 'releases',
    label: 'Release Center',
    path: '/apps/studio',
    icon: Rocket,
  },
];

interface StudioNavigationProps {
  items?: StudioNavItem[];
  className?: string;
  collapsed?: boolean;
}

/**
 * Studio Navigation Component
 * Unified left navigation for all Studio screens
 */
export function StudioNavigation({
  items = defaultStudioNavigation,
  className,
  collapsed = false,
}: StudioNavigationProps) {
  return (
    <nav
      className={cn(
        'flex flex-col bg-surface-subtle border-r border-outline/20',
        collapsed ? 'w-16' : 'w-56',
        'transition-all duration-200',
        className
      )}
    >
      {/* Logo / Brand */}
      <div className="h-14 flex items-center px-4 border-b border-outline/20">
        {collapsed ? (
          <div className="w-8 h-8 rounded-md bg-primary/10 flex items-center justify-center">
            <span className="text-primary font-bold text-sm">S</span>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-md bg-primary/10 flex items-center justify-center">
              <span className="text-primary font-bold text-sm">S</span>
            </div>
            <span className="font-semibold text-foreground">Studio</span>
          </div>
        )}
      </div>

      {/* Navigation Items */}
      <div className="flex-1 py-2 overflow-y-auto">
        <div className="px-2 space-y-1">
          {items.map((item) => (
            <NavItem key={item.id} item={item} collapsed={collapsed} />
          ))}
        </div>
      </div>
    </nav>
  );
}

interface NavItemProps {
  item: StudioNavItem;
  collapsed: boolean;
  depth?: number;
}

function NavItem({ item, collapsed, depth = 0 }: NavItemProps) {
  const location = useLocation();
  const [isOpen, setIsOpen] = React.useState(true);
  const hasChildren = item.children && item.children.length > 0;
  
  // Check if any child is active
  const isChildActive = hasChildren && item.children!.some(
    (child) => location.pathname === child.path || location.pathname.startsWith(child.path + '/')
  );
  
  const Icon = item.icon;
  const paddingLeft = collapsed ? 'px-2' : depth > 0 ? 'pl-10 pr-2' : 'px-2';

  if (hasChildren && !collapsed) {
    return (
      <div className="space-y-1">
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            'w-full flex items-center gap-3 py-2 text-sm font-medium rounded-md transition-colors',
            paddingLeft,
            isChildActive
              ? 'text-primary'
              : 'text-muted-foreground hover:bg-surface-raised hover:text-foreground'
          )}
        >
          <Icon className="size-4 shrink-0" />
          <span className="flex-1 text-left truncate">{item.label}</span>
          {isOpen ? (
            <ChevronDown className="size-4 opacity-50" />
          ) : (
            <ChevronRight className="size-4 opacity-50" />
          )}
        </button>
        
        {isOpen && (
          <div className="space-y-1">
            {item.children!.map((child) => (
              <NavItem key={child.id} item={child} collapsed={collapsed} depth={depth + 1} />
            ))}
          </div>
        )}
      </div>
    );
  }

  if (collapsed) {
    return (
      <NavLink
        to={item.path}
        title={item.label}
        className={({ isActive }) =>
          cn(
            'flex items-center justify-center py-2 rounded-md transition-colors',
            paddingLeft,
            isActive
              ? 'bg-primary/10 text-primary'
              : 'text-muted-foreground hover:bg-surface-raised hover:text-foreground'
          )
        }
      >
        <Icon className="size-5" />
      </NavLink>
    );
  }

  return (
    <NavLink
      to={item.path}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-3 py-2 text-sm font-medium rounded-md transition-colors',
          paddingLeft,
          isActive
            ? 'bg-primary/10 text-primary'
            : 'text-muted-foreground hover:bg-surface-raised hover:text-foreground'
        )
      }
    >
      <Icon className="size-4 shrink-0" />
      <span className="truncate">{item.label}</span>
      {item.badge && (
        <span className="ml-auto text-xs bg-primary/10 text-primary px-1.5 py-0.5 rounded">
          {item.badge}
        </span>
      )}
    </NavLink>
  );
}

export default StudioNavigation;
