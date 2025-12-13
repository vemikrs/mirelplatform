import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Button,
  Input,
} from '@mirel/ui';
import { Search, Check } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getTenants } from '../api';
import type { Tenant } from '../api';

interface TenantSelectorDialogProps {
  open: boolean;
  onClose: () => void;
  onSelect: (tenants: Tenant[]) => void;
  multiSelect?: boolean;
  excludedTenantIds?: string[];
}

export const TenantSelectorDialog = ({
  open,
  onClose,
  onSelect,
  multiSelect = true,
  excludedTenantIds = [],
}: TenantSelectorDialogProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTenants, setSelectedTenants] = useState<Map<string, Tenant>>(new Map());

  // ダイアログが開かれたときに選択状態をリセット
  useEffect(() => {
    if (open) {
      setSelectedTenants(new Map());
      setSearchQuery('');
    }
  }, [open]);

  const { data: tenantsResponse, isLoading } = useQuery({
    queryKey: ['admin-tenants', searchQuery],
    queryFn: async () => {
      const res = await getTenants(searchQuery);
      return res.data;
    },
    enabled: open,
  });

  const availableTenants = (tenantsResponse || []).filter(
    (t) => !excludedTenantIds.includes(t.tenantId)
  );

  const handleToggle = (tenant: Tenant) => {
    setSelectedTenants((prev) => {
      const newMap = new Map(prev);
      if (newMap.has(tenant.tenantId)) {
        newMap.delete(tenant.tenantId);
      } else {
        if (!multiSelect) {
          newMap.clear();
        }
        newMap.set(tenant.tenantId, tenant);
      }
      return newMap;
    });
  };

  const handleApply = () => {
    onSelect(Array.from(selectedTenants.values()));
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-xl max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>テナントの選択</DialogTitle>
        </DialogHeader>

        <div className="flex items-center gap-2 mb-2 p-1">
          <div className="relative flex-1">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="テナント名で検索..."
              className="pl-9"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoFocus
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto min-h-[300px] border rounded-md">
          {isLoading ? (
            <div className="flex items-center justify-center h-full text-muted-foreground">
              読み込み中...
            </div>
          ) : availableTenants.length === 0 ? (
            <div className="flex items-center justify-center h-full text-muted-foreground">
              {searchQuery ? 'テナントが見つかりません' : '利用可能なテナントがありません'}
            </div>
          ) : (
            <div className="divide-y">
              {availableTenants.map((tenant) => {
                const isSelected = selectedTenants.has(tenant.tenantId);
                return (
                  <div
                    key={tenant.tenantId}
                    className={`flex items-center gap-3 p-3 cursor-pointer hover:bg-muted/50 transition-colors ${
                      isSelected ? 'bg-primary/5' : ''
                    }`}
                    onClick={() => handleToggle(tenant)}
                  >
                    <div
                      className={`h-5 w-5 rounded border flex items-center justify-center shrink-0 ${
                        isSelected
                          ? 'bg-primary border-primary text-primary-foreground'
                          : 'border-input bg-background'
                      }`}
                    >
                      {isSelected && <Check className="h-3.5 w-3.5" />}
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <div className="font-medium truncate flex items-center gap-2">
                        {tenant.tenantName}
                        {tenant.plan === 'PRO' && <span className="text-xs bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300 px-1.5 py-0.5 rounded">PRO</span>}
                      </div>
                      <div className="text-xs text-muted-foreground truncate">
                        {tenant.domain}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <DialogFooter className="mt-4">
          <div className="flex-1 flex items-center text-sm text-muted-foreground">
            {selectedTenants.size}件選択中
          </div>
          <Button variant="outline" onClick={onClose}>
            キャンセル
          </Button>
          <Button onClick={handleApply} disabled={selectedTenants.size === 0}>
            選択して追加
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
