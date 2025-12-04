import { SectionHeading } from '@mirel/ui';
import { useQuery } from '@tanstack/react-query';
import { getMenuTree } from '@/lib/api/menu';
import { Loader2 } from 'lucide-react';

export function MenuManagementPage() {
  const { data: menus, isLoading } = useQuery({
    queryKey: ['menu-tree'],
    queryFn: getMenuTree,
  });

  return (
    <div className="space-y-6">
      <SectionHeading
        title="メニュー定義管理"
        description="アプリケーションのメニュー構造とアクセス権限を管理します。"
      />

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <div className="rounded-lg border bg-card text-card-foreground shadow-sm">
          <div className="p-6">
            <pre className="text-xs overflow-auto max-h-[500px]">
              {JSON.stringify(menus, null, 2)}
            </pre>
            {/* TODO: Implement Tree View and Editor */}
          </div>
        </div>
      )}
    </div>
  );
}
