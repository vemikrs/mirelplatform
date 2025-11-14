import { Badge, Button, Card, CardContent, CardDescription, CardHeader, CardTitle, SectionHeading } from '@mirel/ui'
import { Link } from 'react-router-dom'
import { ArrowRight, Users2, Workflow, Paintbrush, Building2, LayoutGrid } from 'lucide-react'

const modules = [
  {
    id: 'users',
    title: 'ユーザ & テナント管理',
    description: '認証・権限・テナントスコープを統合的に設定',
    icon: <Users2 className="size-6 text-primary" />,
    status: '準備中',
  },
  {
    id: 'workflow',
    title: '業務ワークフロー',
    description: '承認フローや業務プロセスをノーコード定義',
    icon: <Workflow className="size-6 text-primary" />,
    status: '設計中',
  },
  {
    id: 'themes',
    title: 'テーマスイッチャ',
    description: 'ブランド毎のテーマ・配色を切り替え',
    icon: <Paintbrush className="size-6 text-primary" />,
    status: '実装中',
  },
  {
    id: 'menu',
    title: 'メニュー / ナビ管理',
    description: 'JSON/バックエンド連携でメニューを集中管理',
    icon: <LayoutGrid className="size-6 text-primary" />,
    status: '計画中',
  },
  {
    id: 'context',
    title: 'コンテキスト管理',
    description: '利用者・テナント・業務文脈の状態共有',
    icon: <Building2 className="size-6 text-primary" />,
    status: '研究中',
  },
]

export function HomePage() {
  return (
    <div className="space-y-12">
      <SectionHeading
        eyebrow="mirelplatform"
        title="業務アプリ基盤ポータル"
        description="業務アプリケーション基盤と、近日拡張予定のモジュールをご確認いただけます。"
        actions={
          <div className="flex flex-wrap gap-2">
            <Button asChild size="lg">
              <Link to="/promarker">
                ProMarker を開く
                <ArrowRight className="ml-2 size-4" />
              </Link>
            </Button>
            <Button asChild variant="subtle" size="lg">
              <Link to="/catalog">UI カタログを見る</Link>
            </Button>
          </div>
        }
      />

      <div className="grid gap-6 lg:grid-cols-2">
        {modules.map((module) => (
          <Card key={module.id} data-testid="home-module-card" className="h-full">
            <CardHeader className="flex flex-row items-start justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-primary/10 p-3">{module.icon}</div>
                <div>
                  <CardTitle className="text-xl">{module.title}</CardTitle>
                  <CardDescription>{module.description}</CardDescription>
                </div>
              </div>
              <Badge variant="neutral">{module.status}</Badge>
            </CardHeader>
            <CardContent className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                {module.id === 'users'
                  ? 'ユーザ/組織/ロールの階層管理と監査ログ連携を順次提供予定。'
                  : module.id === 'workflow'
                    ? 'BPMN 連携やWebhookにより複雑な業務シナリオをオーケストレーション。'
                    : module.id === 'themes'
                      ? 'ライト/ダークだけでなくブランド別トークンを切り替え可能に。'
                      : module.id === 'menu'
                        ? 'バックエンド管理 API と JSON 設定でメニュー配信を自動化。'
                        : 'アプリ全体のコンテキストを統合し、業務ステータスを共有。'}
              </p>
              <Button variant="ghost" asChild>
                <Link to="/sitemap">
                  詳細
                  <ArrowRight className="ml-1 size-4" />
                </Link>
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
