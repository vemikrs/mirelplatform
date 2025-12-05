import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from '@mirel/ui';
import { Database, Share2, Tags, ArrowRight } from 'lucide-react';
import { StudioLayout } from '../../layouts/StudioLayout';

export const ModelerHomePage: React.FC = () => {
  const navigate = useNavigate();

  const features = [
    {
      id: 'entities',
      title: 'Entities',
      description: 'Define data models, fields, and validation rules.',
      icon: Database,
      path: '/apps/studio/modeler/entities',
    },
    {
      id: 'relations',
      title: 'Relations',
      description: 'Visualize and manage relationships between entities.',
      icon: Share2,
      path: '/apps/studio/modeler/relations',
    },
    {
      id: 'codes',
      title: 'Code Master',
      description: 'Manage system codes and enumerations.',
      icon: Tags,
      path: '/apps/studio/modeler/codes',
    },
  ];

  return (
    <StudioLayout>
      <div className="p-8 max-w-7xl mx-auto w-full">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold">Modeler Dashboard</h1>
            <p className="text-muted-foreground">Design your data architecture</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {features.map((feature) => (
            <Card 
              key={feature.id}
              className="p-6 hover:shadow-md transition-shadow cursor-pointer group flex flex-col h-full"
              onClick={() => navigate(feature.path)}
            >
              <div className="flex items-start justify-between mb-4">
                <div className="p-2 bg-primary/10 rounded-lg text-primary">
                  <feature.icon className="size-6" />
                </div>
              </div>
              
              <h3 className="font-semibold text-lg mb-2 group-hover:text-primary transition-colors">
                {feature.title}
              </h3>
              <p className="text-sm text-muted-foreground mb-4 flex-1">
                {feature.description}
              </p>

              <div className="flex items-center text-sm font-medium text-primary mt-auto">
                Open {feature.title} <ArrowRight className="ml-2 size-4" />
              </div>
            </Card>
          ))}
        </div>
      </div>
    </StudioLayout>
  );
};
