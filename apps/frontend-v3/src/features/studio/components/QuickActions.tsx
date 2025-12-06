import React from 'react';
import { Button, Card } from '@mirel/ui';
import { Database, Plus, Play, Upload } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const QuickActions: React.FC = () => {
  const navigate = useNavigate();

  const actions = [
    {
      label: 'Open Modeler',
      icon: Database,
      onClick: () => navigate('/apps/studio/modeler'),
      variant: 'default' as const,
    },
    {
      label: 'New Form',
      icon: Plus,
      onClick: () => navigate('/apps/studio/new'),
      variant: 'outline' as const,
    },
    {
      label: 'New Flow',
      icon: Play,
      onClick: () => navigate('/apps/studio/forms/new?mode=flow'),
      variant: 'outline' as const,
    },
    {
      label: 'Release',
      icon: Upload,
      onClick: () => {
        // Release requires context. Navigate to Forms list for now.
        navigate('/apps/studio/forms');
      },
      variant: 'ghost' as const,
    },
  ];

  return (
    <Card className="p-4 mb-8">
      <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
      <div className="flex flex-wrap gap-4">
        {actions.map((action, index) => (
          <Button
            key={index}
            variant={action.variant}
            onClick={action.onClick}
            className="flex items-center gap-2"
          >
            <action.icon className="w-4 h-4" />
            {action.label}
          </Button>
        ))}
      </div>
    </Card>
  );
};
