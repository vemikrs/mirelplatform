import { Badge, Button, Card, CardContent, CardDescription, CardHeader, CardTitle, SectionHeading } from '@mirel/ui'
import { Link } from 'react-router-dom'
import { ArrowRight, Users2, Workflow, Paintbrush, Building2, LayoutGrid, Code, Palette } from 'lucide-react'

const modules = [
  {
    id: 'promarker',
    title: 'ProMarker',
    subtitle: 'コード生成エンジン',
    description: 'FreeMarkerベースの動的コード生成',
    detailDescription: 'テンプレートから業務アプリケーションのソースコードを自動生成。Java/React対応。',
    status: '稼働中',
    icon: <Code className="size-7 text-primary/80" />,
    link: '/promarker',
    featured: true,
  },
  {
    id: 'ui-system',
    title: '@mirel/ui',
    subtitle: 'デザインシステム',
    description: 'shadcn/ui + Radix UI ベースコンポーネント',
    detailDescription: 'React 19対応の包括的UIコンポーネントライブラリ。TypeScript完全対応。',
    status: '稼働中',
    icon: <Palette className="size-7 text-primary/80" />,
    link: '/catalog',
  },
  {
    id: 'users',
    title: 'ユーザ & テナント',
    subtitle: '管理',
    description: '認証・権限・テナントスコープを統合的に設定',
    detailDescription: 'ユーザ/組織/ロールの階層管理と監査ログ連携を順次提供予定。',
    icon: <Users2 className="size-7 text-primary/80" />,
    status: '準備中',
  },
  {
    id: 'workflow',
    title: '業務ワークフロー',
    subtitle: 'プロセス管理',
    description: '承認フローや業務プロセスをノーコード定義',
    detailDescription: 'BPMN 連携やWebhookにより複雑な業務シナリオをオーケストレーション。',
    icon: <Workflow className="size-7 text-primary/80" />,
    status: '設計中',
  },
  {
    id: 'themes',
    title: 'テーマスイッチャ',
    subtitle: 'ブランド対応',
    description: 'ブランド毎のテーマ・配色を切り替え',
    detailDescription: 'ライト/ダークだけでなくブランド別トークンを切り替え可能に。',
    icon: <Paintbrush className="size-7 text-primary/80" />,
    status: '実装中',
  },
  {
    id: 'menu',
    title: 'メニュー / ナビ',
    subtitle: '集中管理',
    description: 'JSON/バックエンド連携でメニューを集中管理',
    detailDescription: 'バックエンド管理 API と JSON 設定でメニュー配信を自動化。',
    icon: <LayoutGrid className="size-7 text-primary/80" />,
    status: '計画中',
  },
  {
    id: 'context',
    title: 'コンテキスト管理',
    subtitle: '業務文脈',
    description: '利用者・テナント・業務文脈の状態共有',
    detailDescription: 'アプリ全体のコンテキストを統合し、業務ステータスを共有。',
    icon: <Building2 className="size-7 text-primary/80" />,
    status: '研究中',
  },
]

export function HomePage() {
  return (
    <div className="space-y-12">
      <SectionHeading
        eyebrow="mirelplatform"
        title="業務アプリ基盤ポータル"
        description="業務アプリケーション基盤と、拡張予定のモジュールをご確認いただけます。"
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

      <div className="grid gap-8 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 auto-rows-fr">
        {modules.map((module) => (
          <Card 
            key={module.id} 
            data-testid="home-module-card" 
            className="group relative overflow-hidden transition-all duration-300 hover:shadow-lg hover:shadow-primary/8 border-outline/20 bg-surface/60 backdrop-blur-sm"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-primary/8 to-primary/2 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
            
            <CardHeader className="relative z-10 pb-4">
              <div className="flex items-center justify-between">
                <div className="rounded-xl bg-primary/8 p-3 group-hover:bg-primary/12 transition-colors">
                  {module.icon}
                </div>
                <Badge 
                  variant={module.status === '稼働中' ? 'success' : 'neutral'}
                  className="border-primary/20 bg-primary/5 text-primary/80"
                >
                  {module.status}
                </Badge>
              </div>
              <div className="space-y-1 pt-4">
                <CardTitle className="text-lg font-semibold text-foreground/90">
                  {module.title}
                </CardTitle>
                {module.subtitle && (
                  <p className="text-xs text-muted-foreground/70">{module.subtitle}</p>
                )}
              </div>
            </CardHeader>
            
            <CardContent className="relative z-10 space-y-3">
              <CardDescription className="text-sm text-muted-foreground/80">
                {module.description}
              </CardDescription>
              
              <p className="text-sm text-foreground/70 leading-relaxed">
                {module.detailDescription}
              </p>
              
              <Button 
                variant="ghost" 
                size="sm" 
                className="mt-4 w-full justify-between text-primary/80 hover:text-primary hover:bg-primary/8"
                asChild
              >
                <Link to={module.link || '/sitemap'}>
                  詳細を見る <ArrowRight className="size-4" />
                </Link>
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}
