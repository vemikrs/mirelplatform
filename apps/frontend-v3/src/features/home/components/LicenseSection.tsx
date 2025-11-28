import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Badge, Card, CardContent, CardHeader, CardTitle, Button } from '@mirel/ui';
import { 
  Shield, 
  Crown, 
  Star, 
  Zap, 
  Clock, 
  ChevronRight,
  RefreshCw,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { getUserLicenses, type LicenseInfo } from '@/lib/api/userProfile';

// License tier configuration
const tierConfig = {
  FREE: { 
    icon: <Star className="size-5" />, 
    color: 'bg-slate-100 text-slate-700', 
    label: 'FREE' 
  },
  TRIAL: { 
    icon: <Clock className="size-5" />, 
    color: 'bg-blue-100 text-blue-700', 
    label: 'TRIAL' 
  },
  BASIC: { 
    icon: <Shield className="size-5" />, 
    color: 'bg-green-100 text-green-700', 
    label: 'BASIC' 
  },
  PROFESSIONAL: { 
    icon: <Crown className="size-5" />, 
    color: 'bg-purple-100 text-purple-700', 
    label: 'PRO' 
  },
  ENTERPRISE: { 
    icon: <Zap className="size-5" />, 
    color: 'bg-amber-100 text-amber-700', 
    label: 'MAX' 
  },
} as const;

// Map backend tier names to our config
function getTierConfig(tier: string) {
  if (tier === 'PRO') return tierConfig.PROFESSIONAL;
  if (tier === 'MAX') return tierConfig.ENTERPRISE;
  return tierConfig[tier as keyof typeof tierConfig] || tierConfig.FREE;
}

interface LicenseCardProps {
  license: LicenseInfo;
}

function LicenseCard({ license }: LicenseCardProps) {
  const tier = getTierConfig(license.tier);
  const expiresAt = license.expiresAt ? new Date(license.expiresAt) : null;
  const isExpiringSoon = expiresAt && (expiresAt.getTime() - Date.now()) < 30 * 24 * 60 * 60 * 1000;

  return (
    <div 
      className="p-4 rounded-xl border bg-card/50 backdrop-blur-sm transition-all hover:shadow-md"
      style={{
        borderColor: 'hsl(var(--outline) / 0.15)',
      }}
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={`p-2 rounded-lg ${tier.color}`}>
            {tier.icon}
          </div>
          <div>
            <h4 className="font-medium text-foreground">{license.applicationId}</h4>
            <div className="flex items-center gap-2 mt-1">
              <Badge variant="outline" className="text-xs">
                {tier.label}
              </Badge>
              <span className="text-xs text-muted-foreground">
                {license.subjectType === 'TENANT' ? 'テナント' : '個人'}
              </span>
            </div>
          </div>
        </div>
        {expiresAt && (
          <div className="text-right">
            <p className={`text-xs ${isExpiringSoon ? 'text-amber-600' : 'text-muted-foreground'}`}>
              {isExpiringSoon && '⚠️ '}
              {expiresAt.toLocaleDateString('ja-JP')} まで
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export function LicenseSection() {
  const { data: licenses, isLoading, error } = useQuery({
    queryKey: ['user-licenses'],
    queryFn: getUserLicenses,
    staleTime: 60 * 1000, // 1 minute
  });

  if (isLoading) {
    return (
      <Card className="bg-card/50 backdrop-blur-sm border-outline/15">
        <CardContent className="flex items-center justify-center py-8">
          <RefreshCw className="size-6 animate-spin text-muted-foreground" />
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return null; // Silently fail for dashboard widget
  }

  if (!licenses || licenses.length === 0) {
    return (
      <Card className="bg-card/50 backdrop-blur-sm border-outline/15">
        <CardHeader className="pb-2">
          <CardTitle className="text-lg flex items-center gap-2">
            <Shield className="size-5 text-primary/70" />
            あなたのライセンス
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-6">
            <p className="text-muted-foreground mb-4">
              現在有効なライセンスがありません
            </p>
            <Button asChild variant="outline">
              <Link to="/pricing">
                ライセンスを取得
                <ChevronRight className="size-4 ml-1" />
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Group licenses by application
  const groupedLicenses = licenses.reduce((acc, license) => {
    const app = license.applicationId;
    if (!acc[app]) acc[app] = [];
    acc[app].push(license);
    return acc;
  }, {} as Record<string, LicenseInfo[]>);

  return (
    <Card 
      className="bg-card/50 backdrop-blur-sm"
      style={{
        borderColor: 'hsl(var(--outline) / 0.15)',
        boxShadow: 'var(--liquid-elevation-floating)',
      }}
    >
      <CardHeader className="pb-2">
        <CardTitle className="text-lg flex items-center justify-between">
          <span className="flex items-center gap-2">
            <Shield className="size-5 text-primary/70" />
            あなたのライセンス
          </span>
          <Badge variant="outline" className="font-normal">
            {licenses.length} 件
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {Object.entries(groupedLicenses).map(([app, appLicenses]) => (
          <React.Fragment key={app}>
            {appLicenses.map((license) => (
              <LicenseCard key={license.id} license={license} />
            ))}
          </React.Fragment>
        ))}
        
        {/* Upgrade CTA */}
        {licenses.some(l => l.tier === 'FREE' || l.tier === 'TRIAL') && (
          <div className="pt-2 border-t border-outline/10">
            <Button 
              asChild 
              variant="ghost" 
              className="w-full justify-between text-primary/80 hover:text-primary"
            >
              <Link to="/pricing">
                <span>プランをアップグレード</span>
                <ChevronRight className="size-4" />
              </Link>
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
