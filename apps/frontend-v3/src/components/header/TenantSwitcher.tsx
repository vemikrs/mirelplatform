import React, { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { Button, Select } from '@mirel/ui';
import { ChevronDown } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getUserTenants, type TenantInfo } from '@/lib/api/userProfile';

/**
 * テナント切替コンポーネント
 */
export function TenantSwitcher() {
  const { currentTenant, switchTenant, tokens } = useAuth();
  const [isLoading, setIsLoading] = useState(false);

  // Fetch user's tenants from API
  const { data: tenants = [] } = useQuery<TenantInfo[]>({
    queryKey: ['userTenants'],
    queryFn: async () => {
      if (!tokens?.accessToken) return [];
      return getUserTenants(tokens.accessToken);
    },
    enabled: !!tokens?.accessToken,
  });

  const handleTenantSwitch = async (tenantId: string) => {
    if (tenantId === currentTenant?.tenantId) return;
    
    setIsLoading(true);
    try {
      await switchTenant(tenantId);
    } catch (error) {
      console.error('Failed to switch tenant:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (!currentTenant || tenants.length === 0) {
    return null;
  }

  return (
    <div className="flex items-center gap-2">
      <select
        value={currentTenant.tenantId}
        onChange={(e) => handleTenantSwitch(e.target.value)}
        disabled={isLoading || tenants.length === 0}
        className="px-3 py-2 border rounded-md bg-white text-sm font-medium flex items-center gap-2 hover:bg-gray-50 disabled:opacity-50"
      >
        {tenants.map((tenant) => (
          <option key={tenant.tenantId} value={tenant.tenantId}>
            {tenant.displayName}
          </option>
        ))}
      </select>
    </div>
  );
}
