import { Link } from 'react-router-dom';
import { Database, List, Tags } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@mirel/ui';
import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';

export const ModelerHomePage: React.FC = () => {
  const menuItems = [
    {
      to: '/apps/studio/modeler/models',
      icon: Database,
      title: 'モデル定義',
      description: 'データモデルとフィールドを定義します。',
    },
    {
      to: '/apps/studio/modeler/records',
      icon: List,
      title: 'データ管理',
      description: '定義されたモデルのレコードを管理します。',
    },
    {
      to: '/apps/studio/modeler/codes',
      icon: Tags,
      title: 'コードマスタ',
      description: '選択肢のコード値を管理します。',
    },
  ];

  return (
    <StudioLayout>
      <div className="flex flex-col h-full overflow-hidden">
        <StudioContextBar
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Modeler', href: '/apps/studio/modeler' },
          ]}
          title="スキーマ管理"
        />

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {menuItems.map((item) => (
              <Link key={item.to} to={item.to}>
                <Card className="h-full hover:bg-accent hover:text-accent-foreground transition-colors cursor-pointer">
                  <CardHeader>
                    <div className="flex items-center gap-3">
                      <item.icon className="h-6 w-6 text-primary" />
                      <CardTitle className="text-xl">{item.title}</CardTitle>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <CardDescription>{item.description}</CardDescription>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </StudioLayout>
  );
};
