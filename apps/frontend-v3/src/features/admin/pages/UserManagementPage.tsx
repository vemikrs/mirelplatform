
import { 
  SectionHeading,
  Card,
  CardHeader,
  CardTitle,
  CardContent
} from '@mirel/ui';
import { Users } from 'lucide-react';

export function UserManagementPage() {
  return (
    <div className="space-y-6">
      <SectionHeading
        eyebrow={
          <span className="inline-flex items-center gap-2">
            <Users className="size-4" />
            システム管理
          </span>
        }
        title="ユーザー・ロール管理"
        description="全テナントを横断して登録ユーザーとロール（権限）を管理します。"
      />

      <Card>
        <CardHeader>
          <CardTitle>ユーザー一覧</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">
            ユーザー管理機能は現在実装中です。
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
