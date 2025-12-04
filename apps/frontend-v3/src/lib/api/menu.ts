import { apiClient } from './client';
import type { NavigationLink } from '@/app/navigation.schema';

export interface MenuDto {
  id: string;
  label: string;
  path: string;
  icon?: string;
  parentId?: string;
  sortOrder?: number;
  requiredPermission?: string;
  description?: string;
  children?: MenuDto[];
}

export async function getMenuTree(): Promise<MenuDto[]> {
  const response = await apiClient.get<MenuDto[]>('/api/v1/menus/tree');
  return response.data;
}

// Adapter to convert MenuDto to NavigationLink if needed, 
// but they are structurally compatible enough for now.
export function adaptMenuToNavigationLink(menu: MenuDto): NavigationLink {
  return {
    id: menu.id,
    label: menu.label,
    path: menu.path || '#',
    icon: menu.icon,
    description: menu.description,
    children: menu.children?.map(adaptMenuToNavigationLink),
    permissions: menu.requiredPermission ? [menu.requiredPermission] : undefined,
  };
}
