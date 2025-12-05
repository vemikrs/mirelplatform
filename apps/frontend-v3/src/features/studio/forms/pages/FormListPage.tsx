import { StudioLayout } from '../../layouts';
import { StudioContextBar } from '../../components';

export const FormListPage: React.FC = () => {
  return (
    <StudioLayout>
      <div className="flex flex-col h-full">
        <StudioContextBar
          title="フォーム一覧"
          breadcrumbs={[
            { label: 'Studio', href: '/apps/studio' },
            { label: 'Forms', href: '/apps/studio/forms' },
          ]}
        />
        <div className="p-6">
          <h1 className="text-2xl font-bold">Forms</h1>
          <p className="text-muted-foreground mt-2">Form list implementation coming soon.</p>
        </div>
      </div>
    </StudioLayout>
  );
};
