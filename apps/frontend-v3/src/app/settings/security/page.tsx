import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@mirel/ui';
import { Input } from '@mirel/ui';
import { FormLabel } from '@mirel/ui';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@mirel/ui';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@mirel/ui';
import { 
  updatePassword, 
  unlinkGitHub, 
  enablePasswordless, 
  setPassword,
  type UpdatePasswordRequest,
  type UserProfile
} from '@/lib/api/userProfile';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  Shield, 
  Key, 
  Github, 
  Mail, 
  Smartphone, 
  Link2, 
  Link2Off, 
  ArrowLeft, 
  Check, 
  AlertTriangle,
  Loader2
} from 'lucide-react';

interface ExtendedUserProfile extends UserProfile {
  oauth2Provider?: string;
  hasPassword?: boolean;
}

interface ApiError {
  response?: {
    data?: {
      error?: string;
    };
  };
  message?: string;
}

export default function SecurityPage() {
  const { user, tokens } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Password change state
  const [formData, setFormData] = useState<UpdatePasswordRequest>({
    currentPassword: '',
    newPassword: '',
  });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordMessage, setPasswordMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // GitHub unlink state
  const [githubDialogOpen, setGithubDialogOpen] = useState(false);
  const [githubUnlinking, setGithubUnlinking] = useState(false);
  const [githubError, setGithubError] = useState<string | null>(null);

  // Passwordless state
  const [passwordlessDialogOpen, setPasswordlessDialogOpen] = useState(false);
  const [passwordlessEnabling, setPasswordlessEnabling] = useState(false);
  const [passwordlessError, setPasswordlessError] = useState<string | null>(null);

  // Set password dialog (for passwordless users)
  const [setPasswordDialogOpen, setSetPasswordDialogOpen] = useState(false);
  const [newPasswordForSet, setNewPasswordForSet] = useState('');
  const [confirmPasswordForSet, setConfirmPasswordForSet] = useState('');
  const [setPasswordError, setSetPasswordError] = useState<string | null>(null);

  const userProfile = user as ExtendedUserProfile | null;
  const hasGitHubLinked = userProfile?.oauth2Provider === 'github';
  const hasPassword = userProfile?.hasPassword !== false;

  const updatePasswordMutation = useMutation({
    mutationFn: async (data: UpdatePasswordRequest) => {
      if (!tokens?.accessToken) throw new Error('Not authenticated');
      return updatePassword(data);
    },
    onSuccess: () => {
      setPasswordMessage({ type: 'success', text: 'パスワードを更新しました' });
      setFormData({ currentPassword: '', newPassword: '' });
      setConfirmPassword('');
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    },
    onError: (error: Error) => {
      setPasswordMessage({ type: 'error', text: error.message });
    },
  });

  const handlePasswordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordMessage(null);

    if (formData.newPassword !== confirmPassword) {
      setPasswordMessage({ type: 'error', text: '新しいパスワードが一致しません' });
      return;
    }

    if (formData.newPassword.length < 8) {
      setPasswordMessage({ type: 'error', text: 'パスワードは8文字以上にしてください' });
      return;
    }

    updatePasswordMutation.mutate(formData);
  };

  // GitHub unlink handler
  const handleUnlinkGitHub = async () => {
    setGithubUnlinking(true);
    setGithubError(null);
    try {
      await unlinkGitHub();
      setGithubDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    } catch (error: unknown) {
      const err = error as ApiError;
      setGithubError(err.response?.data?.error || err.message || 'GitHub連携の解除に失敗しました');
    } finally {
      setGithubUnlinking(false);
    }
  };

  // Passwordless enable handler
  const handleEnablePasswordless = async () => {
    setPasswordlessEnabling(true);
    setPasswordlessError(null);
    try {
      await enablePasswordless();
      setPasswordlessDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    } catch (error: unknown) {
      const err = error as ApiError;
      setPasswordlessError(err.response?.data?.error || err.message || 'パスワードレスログインの有効化に失敗しました');
    } finally {
      setPasswordlessEnabling(false);
    }
  };

  // Set password handler (for passwordless users)
  const handleSetPassword = async () => {
    setSetPasswordError(null);
    
    if (newPasswordForSet !== confirmPasswordForSet) {
      setSetPasswordError('パスワードが一致しません');
      return;
    }
    
    if (newPasswordForSet.length < 8) {
      setSetPasswordError('パスワードは8文字以上にしてください');
      return;
    }

    try {
      await setPassword(newPasswordForSet);
      setSetPasswordDialogOpen(false);
      setNewPasswordForSet('');
      setConfirmPasswordForSet('');
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    } catch (error: unknown) {
      const err = error as ApiError;
      setSetPasswordError(err.response?.data?.error || err.message || 'パスワードの設定に失敗しました');
    }
  };

  // GitHub login URL
  const githubLoginUrl = `${window.location.origin}/mapi/oauth2/authorization/github`;

  return (
    <div className="container mx-auto py-8 max-w-4xl">
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" size="sm" onClick={() => navigate('/settings/profile')}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          戻る
        </Button>
        <h1 className="text-3xl font-bold">セキュリティ設定</h1>
      </div>

      <div className="space-y-6">
        {/* Authentication Methods */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="w-5 h-5" />
              認証方法
            </CardTitle>
            <CardDescription>アカウントへのログイン方法を管理します</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Password Auth */}
            <div className="flex items-center justify-between p-4 border rounded-lg">
              <div className="flex items-center gap-4">
                <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                  <Key className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <p className="font-medium">パスワード認証</p>
                  <p className="text-sm text-muted-foreground">
                    {hasPassword ? 'パスワードでログインできます' : 'パスワードは設定されていません'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {hasPassword ? (
                  <span className="flex items-center gap-1 text-sm text-green-600 dark:text-green-400">
                    <Check className="w-4 h-4" />
                    有効
                  </span>
                ) : (
                  <Button variant="outline" size="sm" onClick={() => setSetPasswordDialogOpen(true)}>
                    設定する
                  </Button>
                )}
              </div>
            </div>

            {/* OTP Auth */}
            <div className="flex items-center justify-between p-4 border rounded-lg">
              <div className="flex items-center gap-4">
                <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                  <Mail className="w-5 h-5 text-purple-600 dark:text-purple-400" />
                </div>
                <div>
                  <p className="font-medium">メール認証 (OTP)</p>
                  <p className="text-sm text-muted-foreground">
                    メールアドレスに送信されるワンタイムパスワードでログイン
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {user?.emailVerified ? (
                  <>
                    <span className="flex items-center gap-1 text-sm text-green-600 dark:text-green-400">
                      <Check className="w-4 h-4" />
                      利用可能
                    </span>
                    {hasPassword && (
                      <Button 
                        variant="outline" 
                        size="sm" 
                        onClick={() => setPasswordlessDialogOpen(true)}
                      >
                        パスワードレスに切替
                      </Button>
                    )}
                  </>
                ) : (
                  <span className="text-sm text-yellow-600 dark:text-yellow-400">
                    メール検証が必要です
                  </span>
                )}
              </div>
            </div>

            {/* GitHub OAuth */}
            <div className="flex items-center justify-between p-4 border rounded-lg">
              <div className="flex items-center gap-4">
                <div className="p-2 bg-gray-100 dark:bg-gray-800 rounded-lg">
                  <Github className="w-5 h-5" />
                </div>
                <div>
                  <p className="font-medium">GitHub連携</p>
                  <p className="text-sm text-muted-foreground">
                    {hasGitHubLinked 
                      ? 'GitHubアカウントでログインできます' 
                      : 'GitHubアカウントと連携してワンクリックログイン'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {hasGitHubLinked ? (
                  <>
                    <span className="flex items-center gap-1 text-sm text-green-600 dark:text-green-400">
                      <Link2 className="w-4 h-4" />
                      連携中
                    </span>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      onClick={() => setGithubDialogOpen(true)}
                      className="text-destructive border-destructive hover:bg-destructive/10"
                    >
                      <Link2Off className="w-4 h-4 mr-2" />
                      解除
                    </Button>
                  </>
                ) : (
                  <Button 
                    variant="outline" 
                    size="sm" 
                    onClick={() => window.location.href = githubLoginUrl}
                  >
                    <Github className="w-4 h-4 mr-2" />
                    連携する
                  </Button>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Password Change */}
        {hasPassword && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Key className="w-5 h-5" />
                パスワード変更
              </CardTitle>
              <CardDescription>アカウントのパスワードを変更できます</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handlePasswordSubmit} className="space-y-6">
                <div className="space-y-2">
                  <FormLabel htmlFor="currentPassword">現在のパスワード *</FormLabel>
                  <Input
                    id="currentPassword"
                    type="password"
                    value={formData.currentPassword}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, currentPassword: e.target.value })}
                    required
                    autoComplete="current-password"
                  />
                </div>

                <div className="space-y-2">
                  <FormLabel htmlFor="newPassword">新しいパスワード *</FormLabel>
                  <Input
                    id="newPassword"
                    type="password"
                    value={formData.newPassword}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, newPassword: e.target.value })}
                    required
                    minLength={8}
                    autoComplete="new-password"
                  />
                  <p className="text-sm text-muted-foreground">8文字以上で入力してください</p>
                </div>

                <div className="space-y-2">
                  <FormLabel htmlFor="confirmPassword">新しいパスワード(確認) *</FormLabel>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={confirmPassword}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setConfirmPassword(e.target.value)}
                    required
                    minLength={8}
                    autoComplete="new-password"
                  />
                </div>

                {passwordMessage && (
                  <div
                    className={`p-3 rounded ${
                      passwordMessage.type === 'success'
                        ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400'
                        : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'
                    }`}
                  >
                    {passwordMessage.text}
                  </div>
                )}

                <Button type="submit" disabled={updatePasswordMutation.isPending}>
                  {updatePasswordMutation.isPending ? '更新中...' : 'パスワードを変更'}
                </Button>
              </form>
            </CardContent>
          </Card>
        )}

        {/* Session Info */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Smartphone className="w-5 h-5" />
              セッション情報
            </CardTitle>
            <CardDescription>現在のログインセッション</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="p-4 bg-muted/50 rounded-lg">
              <p className="text-sm text-muted-foreground">
                最終ログイン: {user?.lastLoginAt 
                  ? new Date(user.lastLoginAt).toLocaleString('ja-JP')
                  : '不明'}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* GitHub Unlink Dialog */}
      <Dialog open={githubDialogOpen} onOpenChange={setGithubDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-yellow-500" />
              GitHub連携を解除
            </DialogTitle>
            <DialogDescription>
              GitHub連携を解除すると、GitHubアカウントでのログインができなくなります。
              {!hasPassword && (
                <span className="block mt-2 text-red-500">
                  パスワードが設定されていないため、連携を解除する前にパスワードを設定してください。
                </span>
              )}
            </DialogDescription>
          </DialogHeader>
          
          {githubError && (
            <div className="p-3 bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400 rounded">
              {githubError}
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setGithubDialogOpen(false)}>
              キャンセル
            </Button>
            <Button 
              variant="destructive" 
              onClick={handleUnlinkGitHub}
              disabled={githubUnlinking || !hasPassword}
            >
              {githubUnlinking ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  解除中...
                </>
              ) : (
                '連携を解除'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Passwordless Enable Dialog */}
      <Dialog open={passwordlessDialogOpen} onOpenChange={setPasswordlessDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>パスワードレスログインに切り替え</DialogTitle>
            <DialogDescription>
              パスワードを削除し、メール認証（OTP）のみでログインするようになります。
              より安全で、パスワードを覚える必要がなくなります。
            </DialogDescription>
          </DialogHeader>
          
          {passwordlessError && (
            <div className="p-3 bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400 rounded">
              {passwordlessError}
            </div>
          )}

          <div className="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg text-sm">
            <p className="font-medium text-blue-800 dark:text-blue-300 mb-2">パスワードレスログインとは</p>
            <ul className="list-disc list-inside text-blue-700 dark:text-blue-400 space-y-1">
              <li>ログイン時にメールアドレスを入力</li>
              <li>ワンタイムパスワードがメールに届く</li>
              <li>そのコードを入力してログイン</li>
            </ul>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setPasswordlessDialogOpen(false)}>
              キャンセル
            </Button>
            <Button 
              onClick={handleEnablePasswordless}
              disabled={passwordlessEnabling}
            >
              {passwordlessEnabling ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  切替中...
                </>
              ) : (
                'パスワードレスに切り替え'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Set Password Dialog (for passwordless users) */}
      <Dialog open={setPasswordDialogOpen} onOpenChange={setSetPasswordDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>パスワードを設定</DialogTitle>
            <DialogDescription>
              新しいパスワードを設定すると、パスワードでもログインできるようになります。
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4">
            <div className="space-y-2">
              <FormLabel htmlFor="newPasswordForSet">新しいパスワード</FormLabel>
              <Input
                id="newPasswordForSet"
                type="password"
                value={newPasswordForSet}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNewPasswordForSet(e.target.value)}
                placeholder="8文字以上"
                autoComplete="new-password"
              />
            </div>
            <div className="space-y-2">
              <FormLabel htmlFor="confirmPasswordForSet">パスワード確認</FormLabel>
              <Input
                id="confirmPasswordForSet"
                type="password"
                value={confirmPasswordForSet}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setConfirmPasswordForSet(e.target.value)}
                placeholder="もう一度入力"
                autoComplete="new-password"
              />
            </div>
            {setPasswordError && (
              <p className="text-sm text-red-600 dark:text-red-400">{setPasswordError}</p>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => {
              setSetPasswordDialogOpen(false);
              setNewPasswordForSet('');
              setConfirmPasswordForSet('');
              setSetPasswordError(null);
            }}>
              キャンセル
            </Button>
            <Button 
              onClick={handleSetPassword}
              disabled={newPasswordForSet.length < 8}
            >
              パスワードを設定
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
