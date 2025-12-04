import { Button, SectionHeading } from '@mirel/ui'
import { Link } from 'react-router-dom'
import { ArrowRight, Sparkles } from 'lucide-react'
import { NotificationList } from '../components/NotificationList'
import { SystemStatusWidget } from '../components/SystemStatusWidget'

export function HomePage() {
  return (
    <div className="space-y-12 pb-16">
      {/* Hero Section with Liquid Glass Effect */}
      <div className="relative overflow-hidden pb-4">
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
          title="ポータル"
        //   description="業務アプリケーション基盤と、拡張予定のモジュールをご確認いただけます。"
          actions={
            <div className="flex flex-wrap gap-3">
              <Button 
                asChild 
                size="lg"
                className="rounded-xl shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-105 px-6 py-3"
              >
                <Link to="/products">
                  製品ラインナップを見る
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

      {/* Dashboard Grid */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Content - Notifications */}
        <div className="lg:col-span-2">
          <NotificationList />
        </div>

        {/* Sidebar - System Status (Admin Only) */}
        {/* TODO: Implement proper role check. For now, showing to all for demo or check user email/role if available */}
        <div className="space-y-6">
          <SystemStatusWidget />
        </div>
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
