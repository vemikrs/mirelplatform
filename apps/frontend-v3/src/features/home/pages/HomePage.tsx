import { Badge, Button, Card, CardContent, CardDescription, CardHeader, CardTitle, SectionHeading } from '@mirel/ui'
import { Link } from 'react-router-dom'
import { ArrowRight, Users2, Workflow, Paintbrush, Building2, LayoutGrid, Code, Palette, Sparkles } from 'lucide-react'

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
    <div className="space-y-12 pb-16">
      {/* Hero Section with Liquid Glass Effect */}
      <div className="relative overflow-hidden">
        {/* Background Gradient Orb */}
        <div 
          className="absolute top-0 right-0 w-[600px] h-[600px] rounded-full opacity-30 blur-3xl pointer-events-none"
          style={{
            background: 'radial-gradient(circle, hsl(var(--primary) / 0.15) 0%, transparent 70%)'
          }}
        />
        
        <SectionHeading
          eyebrow={
            <span className="inline-flex items-center gap-2">
              <Sparkles className="size-4" />
              mirelplatform
            </span>
          }
          title="業務アプリ基盤ポータル"
          description="業務アプリケーション基盤と、拡張予定のモジュールをご確認いただけます。"
          actions={
            <div className="flex flex-wrap gap-3">
              <Button 
                asChild 
                size="lg"
                className="shadow-lg hover:shadow-xl transition-shadow duration-300"
              >
                <Link to="/promarker">
                  ProMarker を開く
                  <ArrowRight className="ml-2 size-4" />
                </Link>
              </Button>
              <Button 
                asChild 
                variant="subtle" 
                size="lg"
                className="backdrop-blur-sm"
              >
                <Link to="/catalog">UI カタログを見る</Link>
              </Button>
            </div>
          }
        />
      </div>

      {/* Module Cards Grid with Liquid Design */}
      <div 
        className="grid gap-6 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 auto-rows-fr"
        style={{
          gridAutoRows: 'minmax(280px, auto)'
        }}
      >
        {modules.map((module) => (
          <Card 
            key={module.id} 
            data-testid="home-module-card" 
            className="group relative overflow-hidden transition-all border-outline/15"
            style={{
              background: 'hsl(var(--surface) / 0.5)',
              backdropFilter: 'blur(12px) saturate(1.8)',
              boxShadow: 'var(--liquid-elevation-floating)',
              borderRadius: 'var(--liquid-radius-xl)',
              transitionDuration: 'var(--liquid-duration-normal)',
              transitionTimingFunction: 'var(--liquid-ease-smooth)'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-4px)';
              e.currentTarget.style.boxShadow = 'var(--liquid-elevation-raised)';
              e.currentTarget.style.background = 'hsl(var(--surface) / 0.7)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = 'var(--liquid-elevation-floating)';
              e.currentTarget.style.background = 'hsl(var(--surface) / 0.5)';
            }}
          >
            {/* Gradient Overlay on Hover */}
            <div 
              className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"
              style={{
                background: 'radial-gradient(circle at top right, hsl(var(--primary) / 0.08) 0%, transparent 70%)',
                transitionDuration: 'var(--liquid-duration-normal)',
                borderRadius: 'inherit'
              }}
            />
            
            <CardHeader className="relative z-10 pb-4">
              <div className="flex items-center justify-between">
                {/* Icon Container with Liquid Glass Effect */}
                <div 
                  className="rounded-xl p-3 transition-all group-hover:scale-105"
                  style={{
                    background: 'hsl(var(--primary) / 0.08)',
                    backdropFilter: 'blur(8px)',
                    transitionDuration: 'var(--liquid-duration-fast)',
                    transitionTimingFunction: 'var(--liquid-ease-bounce)'
                  }}
                >
                  {module.icon}
                </div>
                
                {/* Status Badge */}
                <Badge 
                  variant={module.status === '稼働中' ? 'success' : 'neutral'}
                  className="border-primary/15 text-xs font-medium"
                  style={{
                    background: 'hsl(var(--primary) / 0.06)',
                    backdropFilter: 'blur(8px)'
                  }}
                >
                  {module.status}
                </Badge>
              </div>
              
              <div className="space-y-1.5 pt-4">
                <CardTitle className="text-lg font-semibold text-foreground/90 tracking-tight">
                  {module.title}
                </CardTitle>
                {module.subtitle && (
                  <p className="text-xs text-muted-foreground/70 font-medium">
                    {module.subtitle}
                  </p>
                )}
              </div>
            </CardHeader>
            
            <CardContent className="relative z-10 space-y-4">
              <CardDescription className="text-sm text-muted-foreground/80 leading-relaxed">
                {module.description}
              </CardDescription>
              
              <p className="text-sm text-foreground/70 leading-relaxed">
                {module.detailDescription}
              </p>
              
              {/* Action Button with Liquid Interactive Effect */}
              <Button 
                variant="ghost" 
                size="sm" 
                className="mt-4 w-full justify-between text-primary/80 hover:text-primary group/btn"
                style={{
                  background: 'hsl(var(--primary) / 0)',
                  transitionDuration: 'var(--liquid-duration-fast)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = 'hsl(var(--primary) / 0.08)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'hsl(var(--primary) / 0)';
                }}
                asChild
              >
                <Link to={module.link || '/sitemap'}>
                  <span>詳細を見る</span>
                  <ArrowRight 
                    className="size-4 transition-transform group-hover/btn:translate-x-1" 
                    style={{
                      transitionDuration: 'var(--liquid-duration-fast)'
                    }}
                  />
                </Link>
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Bottom Info Section */}
      <div 
        className="relative mt-16 p-8 rounded-2xl border"
        style={{
          background: 'hsl(var(--surface) / 0.4)',
          backdropFilter: 'blur(16px) saturate(1.8)',
          borderColor: 'hsl(var(--outline) / 0.15)',
          boxShadow: 'var(--liquid-elevation-floating)'
        }}
      >
        <div className="max-w-3xl mx-auto text-center space-y-4">
          <h3 className="text-xl font-semibold text-foreground/90">
            モダンで拡張性の高い業務基盤
          </h3>
          <p className="text-muted-foreground/80 leading-relaxed">
            Liquid Design Systemの原則に基づいた、透明性と流動性を重視したUIデザイン。
            各モジュールは段階的に機能を拡張し、エンタープライズグレードのアプリケーション構築を支援します。
          </p>
        </div>
      </div>
    </div>
  )
}
