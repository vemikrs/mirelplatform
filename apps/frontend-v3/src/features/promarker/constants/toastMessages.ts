export const toastMessages = {
  generateSuccess: {
    title: '生成が完了しました',
    description: '成果物をダウンロードできるようになりました。',
    variant: 'success' as const,
  },
  generateError: {
    title: '生成に失敗しました',
    description: '入力内容を確認し、再度お試しください。',
    variant: 'destructive' as const,
  },
  clearAll: {
    title: '入力をリセットしました',
    description: 'すべてのパラメータを初期化しました。',
    variant: 'info' as const,
  },
  clearStencil: {
    title: 'ステンシル情報を再取得しました',
    description: '最新の定義を取得しました。',
    variant: 'info' as const,
  },
  reloadSuccess: {
    title: 'マスタをリロードしました',
    description: 'ステンシルマスタの更新が完了しました。',
    variant: 'success' as const,
  },
  reloadError: {
    title: 'リロードに失敗しました',
    description: 'ネットワークまたは権限を確認してください。',
    variant: 'destructive' as const,
  },
  jsonApplySuccess: {
    title: 'JSONを適用しました',
    description: 'フォームに JSON の内容を反映しました。',
    variant: 'success' as const,
  },
  jsonApplyError: {
    title: 'JSON適用時にエラーが発生しました',
    description: 'フォーマットを確認してください。',
    variant: 'destructive' as const,
  },
  fileUploadSuccess: {
    title: 'ファイルをアップロードしました',
    description: 'アップロード済みのファイルをフォームに紐付けました。',
    variant: 'success' as const,
  },
  fileUploadError: {
    title: 'ファイルアップロードに失敗しました',
    description: 'ネットワーク状態を確認して再度お試しください。',
    variant: 'destructive' as const,
  },
} as const

export type ToastMessageKey = keyof typeof toastMessages
