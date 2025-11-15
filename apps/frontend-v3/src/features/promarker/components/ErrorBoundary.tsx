import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
  errorInfo: ErrorInfo | null
}

/**
 * Reactエラーバウンダリ
 * コンポーネントツリー内のJavaScriptエラーをキャッチし、
 * クラッシュを防ぐフォールバックUIを表示する
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null
    }
  }
  
  static getDerivedStateFromError(error: Error): Partial<State> {
    // エラーが発生したら、次のレンダリングでフォールバックUIを表示
    return {
      hasError: true,
      error
    }
  }
  
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // エラーログを記録（本番環境では外部サービスに送信）
    console.error('ErrorBoundary caught an error:', error, errorInfo)
    
    this.setState({
      error,
      errorInfo
    })
  }
  
  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null
    })
  }
  
  render() {
    if (this.state.hasError) {
      // カスタムフォールバックUIがあればそれを表示
      if (this.props.fallback) {
        return this.props.fallback
      }
      
      // デフォルトエラーUI
      return (
        <div className="min-h-screen flex items-center justify-center p-4">
          <div className="max-w-2xl w-full bg-red-50 border border-red-200 rounded-lg p-6 shadow-lg">
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0">
                <svg className="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-medium text-red-900 mb-2">
                  エラーが発生しました
                </h3>
                <div className="space-y-2">
                  <p className="text-sm text-red-800">
                    {this.state.error?.message || '予期しないエラーが発生しました。'}
                  </p>
                  <p className="text-sm text-red-700">
                    問題が解決しない場合は、管理者に問い合わせてください。
                  </p>
                  
                  {import.meta.env.DEV && this.state.errorInfo && (
                    <details className="mt-4">
                      <summary className="cursor-pointer text-sm font-medium text-red-900">
                        エラー詳細（開発環境のみ）
                      </summary>
                      <pre className="mt-2 text-xs overflow-auto p-2 bg-white rounded border border-red-200">
                        {this.state.errorInfo.componentStack}
                      </pre>
                    </details>
                  )}
                  
                  <button
                    onClick={this.handleReset}
                    className="mt-4 px-4 py-2 bg-white text-red-600 border border-red-600 rounded hover:bg-red-50 transition-colors"
                  >
                    再読み込み
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )
    }
    
    return this.props.children
  }
}
