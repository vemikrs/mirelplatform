import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Card } from '@mirel/ui';
import { Database, Share2, Tags, ArrowRight } from 'lucide-react';
import { StudioLayout } from '../../layouts/StudioLayout';

import { StudioNavigation } from '../../components/StudioNavigation';
import { ModelerTree } from '../components/ModelerTree';

export const ModelerHomePage: React.FC = () => {
  const navigate = useNavigate();

  const features = [
    {
      id: 'entities',
      title: 'エンティティ',
      description: 'データモデル、フィールド、バリデーションルールを定義します。',
      icon: Database,
      path: '/apps/studio/modeler/entities',
    },
    {
      id: 'relations',
      title: 'リレーション',
      description: 'エンティティ間の関係を可視化・管理します。',
      icon: Share2,
      path: '/apps/studio/modeler/relations',
    },
    {
      id: 'codes',
      title: 'コードマスタ',
      description: 'システムコードや列挙値を管理します。',
      icon: Tags,
      path: '/apps/studio/modeler/codes',
    },
  ];

  return (
    <StudioLayout
      navigation={<StudioNavigation className="h-auto shrink-0 max-h-[40%] border-b" />}
      explorer={<ModelerTree className="flex-1" />}
    >
      <div className="p-8 max-w-7xl mx-auto w-full">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold">モデラーダッシュボード</h1>
            <p className="text-muted-foreground">データアーキテクチャを設計します</p>
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
                {feature.title} を開く <ArrowRight className="ml-2 size-4" />
              </div>
            </Card>
          ))}
        </div>
      </div>
    </StudioLayout>
  );
};
