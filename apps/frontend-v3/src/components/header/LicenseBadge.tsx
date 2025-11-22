import React from 'react';
import { useAuth } from '@/hooks/useAuth';
import { Badge } from '@mirel/ui';
import { useQuery } from '@tanstack/react-query';
import { getUserLicenses, type LicenseInfo } from '@/lib/api/userProfile';

interface LicenseBadgeProps {
  applicationId?: string;
}

type LicenseTier = 'FREE' | 'TRIAL' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';

/**
 * ライセンス表示バッジコンポーネント
 */
export function LicenseBadge({ applicationId = 'promarker' }: LicenseBadgeProps) {
  const { tokens } = useAuth();

  // Fetch user's licenses from API
  const { data: licenses = [] } = useQuery<LicenseInfo[]>({
    queryKey: ['userLicenses'],
    queryFn: async () => {
      if (!tokens?.accessToken) return [];
      return getUserLicenses(tokens.accessToken);
    },
    enabled: !!tokens?.accessToken,
  });

  // Find license for this application
  const currentLicense = licenses.find((license) => license.applicationId === applicationId);
  const currentTier: LicenseTier = currentLicense?.tier || 'FREE';

  const tierColors: Record<LicenseTier, string> = {
    FREE: 'bg-gray-100 text-gray-800',
    TRIAL: 'bg-green-100 text-green-800',
    BASIC: 'bg-blue-100 text-blue-800',
    PROFESSIONAL: 'bg-purple-100 text-purple-800',
    ENTERPRISE: 'bg-yellow-100 text-yellow-800',
  };

  const tierLabels: Record<LicenseTier, string> = {
    FREE: 'Free',
    TRIAL: 'Trial',
    BASIC: 'Basic',
    PROFESSIONAL: 'Pro',
    ENTERPRISE: 'Enterprise',
  };

  return (
    <Badge className={tierColors[currentTier]}>
      {tierLabels[currentTier]}
    </Badge>
  );
}
