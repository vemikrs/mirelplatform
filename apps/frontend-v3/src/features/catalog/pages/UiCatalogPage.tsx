import {
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  FormError,
  FormField,
  FormHelper,
  FormLabel,
  FormRequiredMark,
  Input,
  SectionHeading,
  Skeleton,
  StepIndicator,
} from '@mirel/ui'
import { toast } from '@mirel/ui'

const sampleSteps = [
  { id: 'select', title: 'ステンシル選択', state: 'complete' as const },
  { id: 'details', title: '詳細設定', state: 'current' as const },
  { id: 'execute', title: '生成実行', state: 'upcoming' as const },
]

export function UiCatalogPage() {
  const showToast = () => {
    toast({
      title: '通知サンプル',
      description: 'UI カタログから呼び出した通知です。',
      variant: 'info',
    })
  }

  return (
    <div className="space-y-10">
      <SectionHeading
        eyebrow="Design System"
        title="UI コンポーネントカタログ"
        description="エンタープライズ向け UI 基盤の主要コンポーネントをプレビューできます。"
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card data-testid="catalog-demo-card">
          <CardHeader>
            <CardTitle>アクションボタン</CardTitle>
            <CardDescription>ボタンのバリアントとサイズの組み合わせ例です。</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-wrap gap-3">
              <Button>プライマリ</Button>
              <Button variant="secondary">セカンダリ</Button>
              <Button variant="subtle">サブトル</Button>
              <Button variant="soft">ソフト</Button>
              <Button variant="elevated">エレベーテッド</Button>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <Button size="sm">小サイズ</Button>
              <Button size="pill" variant="soft">
                ピル型
              </Button>
              <Button size="square" variant="ghost" aria-label="メニュー">
                ≡
              </Button>
              <Button variant="outline" onClick={showToast}>
                トーストを表示
              </Button>
            </div>
          </CardContent>
          <CardFooter className="bg-transparent">
            <span className="text-xs text-muted-foreground">バリアントやサイズは Tailwind トークンで一元管理しています。</span>
          </CardFooter>
        </Card>

        <Card data-testid="catalog-demo-card">
          <CardHeader>
            <CardTitle>フォームフィールド</CardTitle>
            <CardDescription>必須・補足・エラー表現を統一した入力欄です。</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <FormField>
              <FormLabel htmlFor="name" requiredMark={<FormRequiredMark />}>項目名</FormLabel>
              <Input id="name" placeholder="値を入力" />
              <FormHelper>システム内部名称。小文字英数字で入力してください。</FormHelper>
            </FormField>
            <FormField>
              <FormLabel htmlFor="code">コード</FormLabel>
              <Input id="code" placeholder="例: PROC-01" />
              <FormError>コードは英数字 8 文字以内で入力してください。</FormError>
            </FormField>
            <div className="flex items-center gap-2">
              <Badge variant="info">検証済み</Badge>
              <Badge variant="success">有効</Badge>
              <Badge variant="warning">注意</Badge>
            </div>
          </CardContent>
        </Card>

        <Card data-testid="catalog-demo-card">
          <CardHeader>
            <CardTitle>ステップインジケータ</CardTitle>
            <CardDescription>業務プロセスの進行状況を視覚化します。</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <StepIndicator steps={sampleSteps} />
            <Skeleton className="h-12 w-full" />
            <p className="text-sm text-muted-foreground">
              スケルトンは読み込み中のプレースホルダとして利用できます。
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
