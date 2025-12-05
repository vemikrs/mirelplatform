import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { ChevronDown, ChevronRight, type LucideIcon } from 'lucide-react';
import { cn } from '@mirel/ui';

export interface StudioNavItem {
  id: string;
  label: string;
  icon?: LucideIcon;
  path?: string;
  children?: StudioNavItem[];
}

export const defaultStudioNavigation: StudioNavItem[] = [
  // Placeholder structure - will be dynamic or passed as props
  {
    id: 'dashboard',
    label: 'Dashboard',
    path: '/studio',
  }
];

interface StudioNavigationProps {
  items?: StudioNavItem[];
  className?: string;
}

export const StudioNavigation: React.FC<StudioNavigationProps> = ({
  items = defaultStudioNavigation,
  className,
}) => {
  const location = useLocation();

  return (
    <nav className={cn("flex-1 overflow-y-auto py-2", className)}>
      <ul className="space-y-1 px-2">
        {items.map((item) => (
          <NavigationItem key={item.id} item={item} currentPath={location.pathname} />
        ))}
      </ul>
    </nav>
  );
};

interface NavigationItemProps {
  item: StudioNavItem;
  currentPath: string;
  level?: number;
}

const NavigationItem: React.FC<NavigationItemProps> = ({ item, currentPath, level = 0 }) => {
  const [isOpen, setIsOpen] = useState(true); // Default open for now
  const hasChildren = item.children && item.children.length > 0;
  
  const Icon = item.icon;

  if (hasChildren) {
    return (
      <li>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            "flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm font-medium hover:bg-muted/50 transition-colors text-muted-foreground hover:text-foreground",
            level > 0 && "pl-8"
          )}
        >
          {isOpen ? <ChevronDown className="size-4 shrink-0" /> : <ChevronRight className="size-4 shrink-0" />}
          {Icon && <Icon className="size-4 shrink-0" />}
          <span>{item.label}</span>
        </button>
        {isOpen && (
          <ul className="mt-1 space-y-1">
            {item.children!.map((child) => (
              <NavigationItem key={child.id} item={child} currentPath={currentPath} level={level + 1} />
            ))}
          </ul>
        )}
      </li>
    );
  }

  return (
    <li>
      <NavLink
        to={item.path || '#'}
        end
        className={({ isActive: isLinkActive }) => cn(
          "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
          level > 0 && "pl-8",
          isLinkActive 
            ? "bg-primary/10 text-primary" 
            : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
        )}
      >
        {Icon && <Icon className="size-4 shrink-0" />}
        <span>{item.label}</span>
      </NavLink>
    </li>
  );
};
