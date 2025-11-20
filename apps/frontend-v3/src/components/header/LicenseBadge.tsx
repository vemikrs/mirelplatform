import React from 'react';
import { useLicense } from '@/hooks/useAuth';
import { Badge } from '@mirel/ui';

interface LicenseBadgeProps {
  applicationId?: string;
}

/**
 * ライセンス表示バッジコンポーネント
 */
export function LicenseBadge({ applicationId = 'promarker' }: LicenseBadgeProps) {
  const { currentTier } = useLicense(applicationId);

  const tierColors = {
    FREE: 'bg-gray-100 text-gray-800',
    PRO: 'bg-blue-100 text-blue-800',
    MAX: 'bg-yellow-100 text-yellow-800',
  };

  const tierLabels = {
    FREE: 'Free',
    PRO: 'Pro',
    MAX: 'Max',
  };

  return (
    <Badge className={tierColors[currentTier]}>
      {tierLabels[currentTier]}
    </Badge>
  );
}
