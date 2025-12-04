import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Wand2, 
  Hammer, 
  GitFork, 
  Settings, 
  ChevronRight, 
  ChevronDown,
  type LucideIcon 
} from 'lucide-react';
import { cn } from '@mirel/ui'; // Assuming cn utility is exported or I can use clsx/tailwind-merge directly if not
import type { NavigationLink } from '@/app/navigation.schema';

// Icon mapping
const ICON_MAP: Record<string, LucideIcon> = {
  'layout-dashboard': LayoutDashboard,
  'wand-2': Wand2,
  'hammer': Hammer,
  'git-fork': GitFork,
  'settings': Settings,
};

interface SideNavigationProps {
  items: NavigationLink[];
  className?: string;
}

export function SideNavigation({ items, className }: SideNavigationProps) {
  return (
    <nav className={cn("w-56 bg-surface-subtle border-r border-outline/20 flex flex-col py-4", className)}>
      <div className="px-3 space-y-1">
        {items.map((item) => (
          <NavItem key={item.id} item={item} />
        ))}
      </div>
    </nav>
  );
}

function NavItem({ item, depth = 0 }: { item: NavigationLink; depth?: number }) {
  const Icon = item.icon ? ICON_MAP[item.icon] : null;
  const hasChildren = item.children && item.children.length > 0;
  const [isOpen, setIsOpen] = useState(true); // Default open for now as per design "Expanded fixed"

  // Indentation for hierarchy
  const paddingLeft = depth > 0 ? `${depth * 1.5 + 0.75}rem` : '0.75rem';

  if (hasChildren) {
    return (
      <div className="space-y-1">
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            "w-full flex items-center gap-3 px-3 py-1.5 text-sm font-medium rounded-md transition-all duration-200",
            "text-muted-foreground hover:bg-surface-raised hover:text-foreground"
          )}
          style={{ paddingLeft }}
        >
          {Icon && <Icon className="size-4" />}
          <span className="flex-1 text-left">{item.label}</span>
          {isOpen ? <ChevronDown className="size-4 opacity-50" /> : <ChevronRight className="size-4 opacity-50" />}
        </button>
        
        {isOpen && (
          <div className="space-y-1">
            {item.children!.map((child) => (
              <NavItem key={child.id} item={child} depth={depth + 1} />
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
          "flex items-center gap-3 px-3 py-1.5 text-sm font-medium rounded-md transition-all duration-200",
          isActive
            ? "bg-primary/10 text-primary"
            : "text-muted-foreground hover:bg-surface-raised hover:text-foreground"
        )
      }
      style={{ paddingLeft }}
    >
      {Icon && <Icon className="size-4" />}
      <span>{item.label}</span>
    </NavLink>
  );
}
