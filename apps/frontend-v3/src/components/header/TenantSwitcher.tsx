import React, { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { Button, Select } from '@mirel/ui';
import { ChevronDown } from 'lucide-react';

/**
 * テナント切替コンポーネント
 */
export function TenantSwitcher() {
  const { currentTenant, switchTenant } = useAuth();
  const [isLoading, setIsLoading] = useState(false);

  // TODO: Fetch user's tenants from API
  const tenants = [
    { tenantId: 'default', tenantName: 'Default Workspace', displayName: 'Default Workspace' },
  ];

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

  if (!currentTenant) {
    return null;
  }

  return (
    <div className="flex items-center gap-2">
      <select
        value={currentTenant.tenantId}
        onChange={(e) => handleTenantSwitch(e.target.value)}
        disabled={isLoading}
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
