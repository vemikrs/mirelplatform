import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Badge, Card, CardContent, CardHeader, CardTitle } from '@mirel/ui';
import { 
  CheckCircle, 
  Beaker, 
  AlertTriangle, 
  Clock, 
  Archive,
  Layers,
} from 'lucide-react';
import { 
  getAvailableFeatures, 
  getInDevelopmentFeatures,
  type FeatureFlag, 
  type FeatureStatus 
} from '@/lib/api/features';

// Status configuration
const statusConfig: Record<FeatureStatus, { icon: React.ReactNode; color: 'neutral' | 'outline' | 'destructive' | 'info' | 'success' | 'warning'; label: string }> = {
  STABLE: { icon: <CheckCircle className="size-3.5" />, color: 'success', label: '安定版' },
  BETA: { icon: <Beaker className="size-3.5" />, color: 'warning', label: 'ベータ' },
  ALPHA: { icon: <AlertTriangle className="size-3.5" />, color: 'warning', label: 'アルファ' },
  PLANNING: { icon: <Clock className="size-3.5" />, color: 'neutral', label: '計画中' },
  DEPRECATED: { icon: <Archive className="size-3.5" />, color: 'destructive', label: '非推奨' },
};

interface FeatureItemProps {
  feature: FeatureFlag;
  showApp?: boolean;
}

function FeatureItem({ feature, showApp = false }: FeatureItemProps) {
  const status = statusConfig[feature.status];
  
  return (
    <div className="flex items-center justify-between py-2 px-3 rounded-lg hover:bg-accent/50 transition-colors">
      <div className="flex items-center gap-3 min-w-0">
        <div className="flex-shrink-0">
          {status.icon}
        </div>
        <div className="min-w-0">
          <p className="font-medium text-sm truncate">{feature.featureName}</p>
          {showApp && (
            <p className="text-xs text-muted-foreground">{feature.applicationId}</p>
          )}
        </div>
      </div>
      <div className="flex items-center gap-2 flex-shrink-0">
        {feature.inDevelopment && (
          <Badge variant="warning" className="text-xs gap-1">
            <Clock className="size-3" />
            開発中
          </Badge>
        )}
        <Badge 
          variant={status.color} 
          className="text-xs"
        >
          {status.label}
        </Badge>
        {feature.requiredLicenseTier && (
          <Badge variant="outline" className="text-xs">
            {feature.requiredLicenseTier}
          </Badge>
        )}
      </div>
    </div>
  );
}

export function UnifiedFeatureSection() {
  const { data: availableFeatures } = useQuery({
    queryKey: ['available-features'],
    queryFn: getAvailableFeatures,
    staleTime: 60 * 1000,
  });

  const { data: inDevFeatures } = useQuery({
    queryKey: ['in-development-features'],
    queryFn: getInDevelopmentFeatures,
    staleTime: 60 * 1000,
  });

  // Static features for Studio and Workflow
  const staticFeatures: FeatureFlag[] = [
    {
      id: 'studio-modeler',
      featureKey: 'studio.modeler',
      featureName: 'Modeler (データモデル定義)',
      applicationId: 'mirel Studio',
      status: 'BETA',
      inDevelopment: true,
      enabledByDefault: false,
      rolloutPercentage: 0,
    },
    {
      id: 'studio-form',
      featureKey: 'studio.form',
      featureName: 'Form Designer (画面作成)',
      applicationId: 'mirel Studio',
      status: 'ALPHA',
      inDevelopment: true,
      enabledByDefault: false,
      rolloutPercentage: 0,
    },
    {
      id: 'studio-flow',
      featureKey: 'studio.flow',
      featureName: 'Flow Designer (ロジック定義)',
      applicationId: 'mirel Studio',
      status: 'PLANNING',
      inDevelopment: true,
      enabledByDefault: false,
      rolloutPercentage: 0,
    },
    {
      id: 'studio-data',
      featureKey: 'studio.data',
      featureName: 'Data Browser (データ管理)',
      applicationId: 'mirel Studio',
      status: 'ALPHA',
      inDevelopment: true,
      enabledByDefault: false,
      rolloutPercentage: 0,
    },
    {
      id: 'studio-release',
      featureKey: 'studio.release',
      featureName: 'Release Center (リリース管理)',
      applicationId: 'mirel Studio',
      status: 'PLANNING',
      inDevelopment: true,
      enabledByDefault: false,
      rolloutPercentage: 0,
    },
    {
      id: 'workflow-process',
      featureKey: 'workflow.process',
      featureName: 'Process Management (BPMN/Webhook)',
      applicationId: 'Business Workflow',
      status: 'PLANNING',
      inDevelopment: true,
      enabledByDefault: false,
      rolloutPercentage: 0,
    },
  ];

  const allFeatures = [
    ...(availableFeatures || []),
    ...(inDevFeatures || []),
    ...staticFeatures
  ];

  // Group by application
  const grouped = allFeatures.reduce((acc, feature) => {
    // Normalize application names if needed
    let app = feature.applicationId;
    if (!acc[app]) acc[app] = [];
    // Avoid duplicates if static features overlap with fetched ones
    if (!acc[app].some(f => f.featureKey === feature.featureKey)) {
      acc[app].push(feature);
    }
    return acc;
  }, {} as Record<string, FeatureFlag[]>);

  // Define display order for applications
  const appOrder = ['mirelplatform', 'promarker', 'mirel Studio', 'Business Workflow'];
  
  const sortedApps = Object.keys(grouped).sort((a, b) => {
    const indexA = appOrder.indexOf(a);
    const indexB = appOrder.indexOf(b);
    if (indexA !== -1 && indexB !== -1) return indexA - indexB;
    if (indexA !== -1) return -1;
    if (indexB !== -1) return 1;
    return a.localeCompare(b);
  });

  return (
    <>
      {sortedApps.map(app => (
        <Card 
          key={app}
          className="bg-card/50 backdrop-blur-sm h-full"
          style={{
            borderColor: 'hsl(var(--outline) / 0.15)',
            boxShadow: 'var(--liquid-elevation-floating)',
          }}
        >
          <CardHeader className="pb-2">
            <CardTitle className="text-lg flex items-center justify-between">
              <span className="flex items-center gap-2">
                <Layers className="size-5 text-primary/70" />
                {app}
              </span>
              <Badge variant="outline" className="font-normal">
                {grouped[app]?.length || 0} 件
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-1">
            {grouped[app]?.map((feature) => (
              <FeatureItem key={feature.id} feature={feature} />
            ))}
          </CardContent>
        </Card>
      ))}
    </>
  );
}
