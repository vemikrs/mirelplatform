import { Card } from '@mirel/ui';
import { Database, Layout, Workflow, Rocket } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getSchemas } from '@/lib/api/schema';

// ... (DashboardStat remains same)
interface DashboardStatProps {
  label: string;
  value: number | string;
  icon: React.ElementType;
  color?: string;
}

const DashboardStat: React.FC<DashboardStatProps> = ({ label, value, icon: Icon, color = "text-primary" }) => (
  <Card className="p-4 flex items-center space-x-4">
    <div className={`p-3 rounded-full bg-muted/20 ${color}`}>
      <Icon className="w-6 h-6" />
    </div>
    <div>
      <p className="text-sm font-medium text-muted-foreground">{label}</p>
      <h3 className="text-2xl font-bold">{value}</h3>
    </div>
  </Card>
);

export const WorkspaceDashboard: React.FC = () => {
  // Fetch schemas to calculate stats (mocking others for now)
  const { data: schemas } = useQuery({
    queryKey: ['studio-schemas'],
    queryFn: getSchemas,
  });

  const entityCount = schemas?.data?.length || 0;
  // Mock counts until APIs are available
  const formCount = schemas?.data?.length || 0; // Assuming 1:1 for now
  const flowCount = 0;
  const releaseCount = 0;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      <DashboardStat 
        label="Entities" 
        value={entityCount} 
        icon={Database} 
        color="text-blue-500" 
      />
      <DashboardStat 
        label="Forms" 
        value={formCount} 
        icon={Layout} 
        color="text-green-500" 
      />
      <DashboardStat 
        label="Flows" 
        value={flowCount} 
        icon={Workflow} 
        color="text-purple-500" 
      />
      <DashboardStat 
        label="Releases" 
        value={releaseCount} 
        icon={Rocket} 
        color="text-orange-500" 
      />
    </div>
  );
};
