import { Button } from '@mirel/ui';
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
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold">フォーム</h1>
            {/* TODO: Add onClick handler for creation */}
            <Button>新規作成</Button>
          </div>
          <p className="text-muted-foreground">フォーム一覧機能は実装中です。</p>
        </div>
      </div>
    </StudioLayout>
  );
};
