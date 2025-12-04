import { Card, CardContent, CardHeader, CardTitle } from '@mirel/ui';
import { Bell, Info, AlertTriangle, CheckCircle } from 'lucide-react';

interface Notification {
  id: string;
  title: string;
  content: string;
  date: string;
  severity: 'info' | 'warning' | 'success' | 'error';
}

const MOCK_NOTIFICATIONS: Notification[] = [
  {
    id: '1',
    title: 'システムメンテナンスのお知らせ',
    content: '2025年12月10日 22:00〜24:00の間、定期メンテナンスを実施します。この間、システムにアクセスできなくなる場合があります。',
    date: '2025-12-01',
    severity: 'warning',
  },
  {
    id: '2',
    title: '新機能リリース: mirel Studio Beta',
    content: 'データモデリング機能を含むmirel Studioのベータ版が利用可能になりました。',
    date: '2025-11-28',
    severity: 'info',
  },
  {
    id: '3',
    title: 'セキュリティアップデート完了',
    content: '最新のセキュリティパッチを適用しました。',
    date: '2025-11-25',
    severity: 'success',
  },
];

const severityConfig = {
  info: { icon: Info, color: 'text-blue-500', bg: 'bg-blue-50 dark:bg-blue-900/20' },
  warning: { icon: AlertTriangle, color: 'text-amber-500', bg: 'bg-amber-50 dark:bg-amber-900/20' },
  success: { icon: CheckCircle, color: 'text-green-500', bg: 'bg-green-50 dark:bg-green-900/20' },
  error: { icon: AlertTriangle, color: 'text-red-500', bg: 'bg-red-50 dark:bg-red-900/20' },
};

export function NotificationList() {
  return (
    <Card className="h-full bg-card/50 backdrop-blur-sm border-outline/15 shadow-sm">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg flex items-center gap-2">
          <Bell className="size-5 text-primary" />
          お知らせ
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {MOCK_NOTIFICATIONS.map((notification) => {
          const config = severityConfig[notification.severity];
          const Icon = config.icon;
          
          return (
            <div 
              key={notification.id} 
              className="flex gap-4 p-3 rounded-lg hover:bg-surface-subtle transition-colors border border-transparent hover:border-outline/10"
            >
              <div className={`p-2 rounded-full h-fit ${config.bg}`}>
                <Icon className={`size-4 ${config.color}`} />
              </div>
              <div className="flex-1 space-y-1">
                <div className="flex items-start justify-between gap-2">
                  <h4 className="text-sm font-medium leading-none mt-1">
                    {notification.title}
                  </h4>
                  <span className="text-xs text-muted-foreground whitespace-nowrap">
                    {notification.date}
                  </span>
                </div>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  {notification.content}
                </p>
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
