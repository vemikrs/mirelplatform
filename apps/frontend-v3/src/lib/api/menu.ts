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

export async function getMenu(id: string): Promise<MenuDto> {
  const response = await apiClient.get<MenuDto>(`/api/v1/menus/${id}`);
  return response.data;
}

export async function createMenu(menu: MenuDto): Promise<void> {
  await apiClient.post('/api/v1/menus', menu);
}

export async function updateMenu(id: string, menu: MenuDto): Promise<void> {
  await apiClient.put(`/api/v1/menus/${id}`, menu);
}

export async function deleteMenu(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/menus/${id}`);
}

export async function updateMenuTree(menus: MenuDto[]): Promise<void> {
  await apiClient.put('/api/v1/menus/tree', menus);
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
