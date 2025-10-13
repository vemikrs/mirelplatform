import { useState } from 'react'
import { Button } from '@mirel/ui'
import { Input } from '@mirel/ui'
import { Toaster, toast } from '@mirel/ui'
import './App.css'

function App() {
  const [message, setMessage] = useState('')

  const handleShowToast = () => {
    toast({
      title: 'メッセージ送信',
      description: message || 'Hello from @mirel/ui!',
      variant: 'default',
    })
  }

  return (
    <>
      <div className="container mx-auto p-8 space-y-6">
        <div className="space-y-2">
          <h1 className="text-4xl font-bold text-foreground">
            ProMarker Platform
          </h1>
          <p className="text-muted-foreground">
            React + Vite + Tailwind CSS + @mirel/ui Design System
          </p>
        </div>

        <div className="space-y-4 max-w-md">
          <div className="space-y-2">
            <label htmlFor="message" className="text-sm font-medium">
              メッセージ
            </label>
            <Input
              id="message"
              placeholder="メッセージを入力してください"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
            />
          </div>

          <div className="flex gap-2">
            <Button onClick={handleShowToast}>
              トースト表示
            </Button>
            <Button variant="secondary" onClick={() => setMessage('')}>
              クリア
            </Button>
            <Button variant="outline" onClick={() => console.log('ProMarker')}>
              ログ出力
            </Button>
          </div>
        </div>

        <div className="text-sm text-muted-foreground">
          <p>Phase 0: pnpm workspace + frontend-v3 + @mirel/ui 初期化完了</p>
          <p>Next: Phase 1 - ProMarker core feature migration</p>
        </div>
      </div>
      <Toaster />
    </>
  )
}

export default App
