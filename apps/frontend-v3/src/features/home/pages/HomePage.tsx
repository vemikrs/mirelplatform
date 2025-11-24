import { Badge, Button, Card, CardContent, CardDescription, CardHeader, CardTitle, SectionHeading } from '@mirel/ui'
import { Link } from 'react-router-dom'
import { ArrowRight, Workflow, Code, Sparkles } from 'lucide-react'

const modules = [
  {
    id: 'promarker',
    title: 'ProMarker',
    subtitle: 'テンプレート駆動型コード生成',
    description: 'FreeMarker エンジンによる高度なコード生成システム',
    detailDescription: 'カスタマイズ可能なテンプレートから、業務アプリケーションの定型コード・機能スケルトン・プロジェクト構成を自動生成。開発の高速化と品質の平準化を実現します。',
    status: '稼働中',
    icon: <Code className="size-7 text-primary/80" />,
    link: '/promarker',
    featured: true,
  },
  {
    id: 'workflow',
    title: '業務ワークフロー',
    subtitle: 'プロセス管理基盤（開発中）',
    description: '承認フローや業務プロセスの定義・実行を統合管理',
    detailDescription: 'BPMN 連携やWebhookにより複雑な業務シナリオをオーケストレーション。',
    icon: <Workflow className="size-7 text-primary/80" />,
    status: '設計中',
    link: '/sitemap',
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
                className="rounded-xl shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-105 px-6 py-3"
              >
                <Link to="/promarker">
                  ProMarker を開く
                  <ArrowRight className="ml-2 size-4" />
                </Link>
              </Button>
              <Button 
                asChild 
                variant="outline" 
                size="lg"
                className="rounded-xl backdrop-blur-sm border-2 hover:bg-accent/50 transition-all duration-300 px-6 py-3"
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
                size="default"
                className="mt-4 w-full justify-between text-primary/80 hover:text-primary/90 hover:bg-primary/8 rounded-lg group/btn"
                asChild
              >
                <Link to={module.link || '/sitemap'}>
                  <span>詳細を見る</span>
                  <ArrowRight 
                    className="size-4 transition-transform group-hover/btn:translate-x-1" 
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
            エンタープライズアプリケーションに必要な機能を統合的に提供。
            モジュラー設計により、組織のニーズに合わせて段階的に機能を拡張できます。
          </p>
        </div>
      </div>
    </div>
  )
}
