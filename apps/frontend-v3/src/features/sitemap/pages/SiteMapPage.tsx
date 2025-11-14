import type { NavigationConfig, NavigationLink } from '@/app/navigation.schema'
import { Badge, Card, CardContent, CardDescription, CardHeader, CardTitle, SectionHeading } from '@mirel/ui'
import { Link, useRouteLoaderData } from 'react-router-dom'

function LinkList({ title, description, links }: { title: string; description?: string; links: NavigationLink[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        {description ? <CardDescription>{description}</CardDescription> : null}
      </CardHeader>
      <CardContent className="space-y-3">
        <ul className="space-y-2">
          {links.map((link) => {
            const tone = (link.badge?.tone ?? 'neutral') as 'neutral' | 'info' | 'success' | 'warning'
            const badgeVariant = tone

            return (
              <li key={link.id} className="flex items-center justify-between gap-3">
                {link.external ? (
                  <a
                    href={link.path}
                    target="_blank"
                    rel="noreferrer"
                    className="text-sm font-medium text-primary transition-colors hover:text-primary/80"
                  >
                    {link.label}
                  </a>
                ) : (
                  <Link
                    to={link.path}
                    className="text-sm font-medium text-primary transition-colors hover:text-primary/80"
                  >
                    {link.label}
                  </Link>
                )}
                {link.badge ? <Badge variant={badgeVariant}>{link.badge.label}</Badge> : null}
              </li>
            )
          })}
        </ul>
      </CardContent>
    </Card>
  )
}

export function SiteMapPage() {
  const navigation = useRouteLoaderData('app-root') as NavigationConfig | undefined

  if (!navigation) {
    return null
  }

  return (
    <div className="space-y-10">
      <SectionHeading
        eyebrow="Navigation"
        title="サイトマップ"
        description="mirelplatform の画面構造と主要モジュールを一覧で確認できます。"
      />
      <div className="grid gap-6 md:grid-cols-2">
        <LinkList title="主要機能" description="ヘッダーに表示される主要メニュー" links={navigation.primary} />
        <LinkList title="クイックリンク" description="管理者向けのショートカット" links={navigation.quickLinks} />
        {navigation.inDevelopment && navigation.inDevelopment.length > 0 ? (
          <div className="md:col-span-2">
            <LinkList title="開発中の機能" description="今後リリース予定のモジュール" links={navigation.inDevelopment} />
          </div>
        ) : null}
        {navigation.secondary.length > 0 ? (
          <LinkList title="補助リンク" description="ドキュメントや外部サイト" links={navigation.secondary} />
        ) : null}
      </div>
    </div>
  )
}
