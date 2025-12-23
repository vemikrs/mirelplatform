import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
  Alert,
  AlertDescription,
  AlertTitle,
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  FormError,
  FormField,
  FormHelper,
  FormLabel,
  FormRequiredMark,
  Input,
  ScrollArea,
  SectionHeading,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Separator,
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
  Slider,
  Spinner,
  StepIndicator,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Textarea,
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
  toast,
} from '@mirel/ui'
import { Plus } from 'lucide-react'
import { useState } from 'react'

const sampleSteps = [
  { id: 'select', title: 'ステンシル選択', state: 'complete' as const },
  { id: 'details', title: '詳細設定', state: 'current' as const },
  { id: 'execute', title: '生成実行', state: 'upcoming' as const },
]

export function UiCatalogPage() {
  const [sliderValue, setSliderValue] = useState([50])
  const [switchChecked, setSwitchChecked] = useState(false)

  const showToast = (variant: 'default' | 'info' | 'success' | 'warning' | 'destructive') => {
    toast({
      title: '通知タイトル',
      description: 'ここに通知の詳細メッセージが表示されます。',
      variant,
    })
  }

  return (
    <div className="space-y-12 pb-24">
      <SectionHeading
        eyebrow="Design System"
        title="UI コンポーネントカタログ"
        description="エンタープライズ向け UI 基盤の全コンポーネントリファレンス。"
      />

      {/* 1. Primitives Section */}
      <section className="space-y-6">
        <h2 className="text-2xl font-bold tracking-tight border-b pb-2">Primitives (基本要素)</h2>
        <div className="grid gap-6 lg:grid-cols-2 xl:grid-cols-3">
          
          {/* Buttons */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Buttons</CardTitle>
              <CardDescription>インタラクションの主要なトリガーです。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex flex-wrap gap-4 items-center">
                <Button>Default</Button>
                <Button variant="secondary">Secondary</Button>
                <Button variant="outline">Outline</Button>
                <Button variant="ghost">Ghost</Button>
                <Button variant="destructive">Destructive</Button>
                <Button variant="link">Link</Button>
              </div>
              <Separator />
              <div className="flex flex-wrap gap-4 items-center">
                <Button variant="subtle">Subtle</Button>
                <Button variant="soft">Soft</Button>
                <Button variant="elevated">Elevated</Button>
              </div>
              <Separator />
              <div className="flex flex-wrap gap-4 items-center">
                <Button size="sm">Small</Button>
                <Button size="default">Default</Button>
                <Button size="lg">Large</Button>
                <Button size="icon" variant="outline"><Plus className="size-4" /></Button>
                <Button size="pill">Pill Shape</Button>
              </div>
            </CardContent>
          </Card>

          {/* Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Badges</CardTitle>
              <CardDescription>ステータスやラベルを表示します。</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4">
              <div className="flex flex-wrap gap-2">
                <Badge>Default</Badge>
                <Badge variant="secondary">Secondary</Badge>
                <Badge variant="outline">Outline</Badge>
                <Badge variant="destructive">Critical</Badge>
              </div>
              <div className="flex flex-wrap gap-2">
                <Badge variant="info">Info</Badge>
                <Badge variant="success">Success</Badge>
                <Badge variant="warning">Warning</Badge>
              </div>
            </CardContent>
          </Card>

          {/* Avatars & Tooltips */}
          <Card>
            <CardHeader>
              <CardTitle>Avatars & Tooltips</CardTitle>
              <CardDescription>ユーザー表現と補助情報です。</CardDescription>
            </CardHeader>
            <CardContent className="flex items-center gap-6">
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger>
                    {/* Fixed Avatar API usage */}
                    <Avatar fallback="CN" />
                  </TooltipTrigger>
                  <TooltipContent>Mirel System</TooltipContent>
                </Tooltip>
              </TooltipProvider>

              <Avatar fallback="JD" />

              <div className="flex items-center gap-2">
                 <Spinner size="sm" />
                 <span className="text-sm text-muted-foreground">Loading...</span>
              </div>
            </CardContent>
          </Card>

        </div>
      </section>

      {/* 2. Form Controls Section */}
      <section className="space-y-6">
        <h2 className="text-2xl font-bold tracking-tight border-b pb-2">Form Controls (入力要素)</h2>
        <div className="grid gap-6 lg:grid-cols-2">
          
          {/* Inputs */}
          <Card>
            <CardHeader>
              <CardTitle>Inputs & Textareas</CardTitle>
              <CardDescription>テキスト入力のバリエーションです。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-2">
                <Input type="email" placeholder="Email (Default)" />
              </div>
              <div className="grid gap-2">
                <Input disabled type="email" placeholder="Email (Disabled)" />
              </div>
              <div className="grid gap-2">
                <Textarea placeholder="Type your message here." />
              </div>
            </CardContent>
          </Card>

          {/* Selection Controls */}
          <Card>
            <CardHeader>
              <CardTitle>Selection Controls</CardTitle>
              <CardDescription>選択とトグルのコントロールです。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <FormLabel>Notifications</FormLabel>
                  <FormHelper>Receive daily updates</FormHelper>
                </div>
                <Switch checked={switchChecked} onCheckedChange={setSwitchChecked} />
              </div>
              
              <div className="space-y-2">
                <FormLabel>Volume: {sliderValue}</FormLabel>
                <Slider defaultValue={[50]} max={100} step={1} value={sliderValue} onValueChange={setSliderValue} />
              </div>

              <div className="space-y-2">
                <FormLabel>Role</FormLabel>
                <Select>
                  <SelectTrigger>
                    <SelectValue placeholder="Select a role" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="admin">Administrator</SelectItem>
                    <SelectItem value="editor">Editor</SelectItem>
                    <SelectItem value="viewer">Viewer</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>

          {/* Form Structure */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Form Structure</CardTitle>
              <CardDescription>ラベル、必須マーク、エラーメッセージを含む完全なフォームフィールド。</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-6 md:grid-cols-2">
              <FormField>
                <FormLabel htmlFor="email" requiredMark={<FormRequiredMark />}>Email Address</FormLabel>
                <Input id="email" placeholder="admin@example.com" />
                <FormHelper>組織のメールアドレスを入力してください。</FormHelper>
              </FormField>

              <FormField>
                <FormLabel htmlFor="password" requiredMark={<FormRequiredMark />}>Password</FormLabel>
                <Input id="password" type="password" />
                <FormError>パスワードは8文字以上である必要があります。</FormError>
              </FormField>
            </CardContent>
          </Card>

        </div>
      </section>

      {/* 3. Feedback & Overlays Section */}
      <section className="space-y-6">
        <h2 className="text-2xl font-bold tracking-tight border-b pb-2">Feedback & Overlays (フィードバック)</h2>
        <div className="grid gap-6 lg:grid-cols-3">
          
          {/* Alerts */}
          <div className="space-y-4 lg:col-span-2">
            <Alert>
              <TerminalIcon className="size-4" />
              <AlertTitle>Default Alert</AlertTitle>
              <AlertDescription>
                これは標準的なアラートメッセージです。
              </AlertDescription>
            </Alert>
            <Alert variant="destructive">
              <AlertCircleIcon className="size-4" />
              <AlertTitle>Critical Error</AlertTitle>
              <AlertDescription>
                クリティカルなエラーが発生しました。直ちに対応が必要です。
              </AlertDescription>
            </Alert>
            <div className="grid grid-cols-2 gap-4">
                <Alert variant="info" className="flex items-center justify-center p-4">Info Variant</Alert>
                <Alert variant="warning" className="flex items-center justify-center p-4">Warning Variant</Alert>
            </div>
          </div>

          {/* Toasts */}
          <Card>
            <CardHeader>
              <CardTitle>Toast Notifications</CardTitle>
              <CardDescription>一時的な通知メッセージ。</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              <Button variant="outline" onClick={() => showToast('default')}>Default Toast</Button>
              <Button variant="outline" onClick={() => showToast('success')} className="border-green-200 hover:bg-green-50 dark:border-green-900 dark:hover:bg-green-950">Success Toast</Button>
              <Button variant="outline" onClick={() => showToast('destructive')} className="border-red-200 hover:bg-red-50 dark:border-red-900 dark:hover:bg-red-900">Error Toast</Button>
            </CardContent>
          </Card>

          {/* Dialogs & Sheets */}
          <Card className="lg:col-span-3">
            <CardHeader>
              <CardTitle>Modals & Sheets</CardTitle>
              <CardDescription>オーバーレイによるコンテンツ表示。</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-6">
              
              <Dialog>
                <DialogTrigger asChild>
                  <Button variant="secondary">Open Dialog</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>Confirmation</DialogTitle>
                    <DialogDescription>
                      この操作を実行してもよろしいですか？この操作は取り消せません。
                    </DialogDescription>
                  </DialogHeader>
                  <DialogFooter>
                    <Button variant="outline">Cancel</Button>
                    <Button>Confirm</Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>

              <Sheet>
                <SheetTrigger asChild>
                  <Button variant="secondary">Open Sheet</Button>
                </SheetTrigger>
                <SheetContent>
                  <SheetHeader>
                    <SheetTitle>Edit Profile</SheetTitle>
                    <SheetDescription>
                      プロフィール情報を編集します。完了したら保存をクリックしてください。
                    </SheetDescription>
                  </SheetHeader>
                  <div className="grid gap-4 py-4">
                    <FormField>
                        <FormLabel>Name</FormLabel>
                        <Input defaultValue="Admin User" />
                    </FormField>
                    <FormField>
                        <FormLabel>Username</FormLabel>
                        <Input defaultValue="@admin" />
                    </FormField>
                  </div>
                  <div className="flex justify-end">
                    <Button>Save changes</Button>
                  </div>
                </SheetContent>
              </Sheet>

            </CardContent>
          </Card>
        </div>
      </section>

      {/* 4. Layout & Data Section */}
      <section className="space-y-6">
        <h2 className="text-2xl font-bold tracking-tight border-b pb-2">Layout & Data (レイアウト)</h2>
        
        {/* Step Indicator */}
        <div className="p-6 border rounded-xl bg-card">
            <h3 className="text-sm font-medium text-muted-foreground mb-4">Step Indicator</h3>
            <StepIndicator steps={sampleSteps} />
        </div>

        {/* Tabs & ScrollArea */}
        <div className="grid gap-6 lg:grid-cols-2">
            <Card>
                <CardHeader>
                    <CardTitle>Tabs</CardTitle>
                </CardHeader>
                <CardContent>
                    <Tabs defaultValue="account" className="w-full">
                        <TabsList className="grid w-full grid-cols-2">
                            <TabsTrigger value="account">Account</TabsTrigger>
                            <TabsTrigger value="password">Password</TabsTrigger>
                        </TabsList>
                        <TabsContent value="account">
                            <div className="p-4 border rounded-md mt-2 bg-muted/50 text-sm">Account settings content.</div>
                        </TabsContent>
                        <TabsContent value="password">
                            <div className="p-4 border rounded-md mt-2 bg-muted/50 text-sm">Change password content.</div>
                        </TabsContent>
                    </Tabs>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Scroll Area</CardTitle>
                </CardHeader>
                <CardContent>
                    <ScrollArea className="h-[150px] w-full rounded-md border p-4">
                        <h4 className="mb-4 text-sm font-medium leading-none">Modules</h4>
                        {Array.from({ length: 20 }).map((_, i) => (
                            <div key={i} className="text-sm border-b py-2 last:border-0 hover:bg-muted/50 transition-colors">
                                Module Item {i + 1}
                            </div>
                        ))}
                    </ScrollArea>
                </CardContent>
            </Card>
        </div>

        {/* Accordion */}
        <Card>
            <CardHeader>
                <CardTitle>Accordion</CardTitle>
            </CardHeader>
            <CardContent>
                <Accordion type="single" collapsible className="w-full">
                    <AccordionItem value="item-1">
                        <AccordionTrigger>Is it accessible?</AccordionTrigger>
                        <AccordionContent>
                        Yes. It adheres to the WAI-ARIA design pattern.
                        </AccordionContent>
                    </AccordionItem>
                    <AccordionItem value="item-2">
                        <AccordionTrigger>Is it styled?</AccordionTrigger>
                        <AccordionContent>
                        Yes. It comes with default styles that matches the other components&apos; aesthetic.
                        </AccordionContent>
                    </AccordionItem>
                </Accordion>
            </CardContent>
        </Card>

        {/* Table */}
         <Card>
            <CardHeader>
                <CardTitle>Table</CardTitle>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead className="w-[100px]">Invoice</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead>Method</TableHead>
                            <TableHead className="text-right">Amount</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        <TableRow>
                            <TableCell className="font-medium">INV001</TableCell>
                            <TableCell>Paid</TableCell>
                            <TableCell>Credit Card</TableCell>
                            <TableCell className="text-right">$250.00</TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell className="font-medium">INV002</TableCell>
                            <TableCell>Pending</TableCell>
                            <TableCell>PayPal</TableCell>
                            <TableCell className="text-right">$120.00</TableCell>
                        </TableRow>
                    </TableBody>
                </Table>
            </CardContent>
        </Card>

      </section>

    </div>
  )
}

function TerminalIcon(props: React.ComponentProps<'svg'>) {
    return (
      <svg
        {...props}
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <polyline points="4 17 10 11 4 5" />
        <line x1="12" x2="20" y1="19" y2="19" />
      </svg>
    )
}

function AlertCircleIcon(props: React.ComponentProps<'svg'>) {
    return (
      <svg
        {...props}
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <circle cx="12" cy="12" r="10" />
        <line x1="12" x2="12" y1="8" y2="12" />
        <line x1="12" x2="12.01" y1="16" y2="16" />
      </svg>
    )
}
