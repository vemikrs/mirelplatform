import { useState, useRef, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useAuth } from '@/hooks/useAuth';
import { Button } from '@mirel/ui';
import { Input } from '@mirel/ui';
import { FormLabel } from '@mirel/ui';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@mirel/ui';
import { Avatar } from '@mirel/ui';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@mirel/ui';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@mirel/ui';
import { Textarea } from '@mirel/ui';
import { 
  updateProfile, 
  uploadAvatar, 
  deleteAvatar,
  updateEmail,
  updatePassword,
  unlinkGitHub,
  enablePasswordless,
  setPassword,
  type UpdateProfileRequest,
  type UpdatePasswordRequest,
  type UserProfile
} from '@/lib/api/userProfile';
import { requestOtp, verifyOtp } from '@/lib/api/otp';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { 
  User, 
  Mail, 
  Shield, 
  Camera, 
  Trash2, 
  Check, 
  Loader2,
  Key,
  Github,
  Smartphone,
  Link2,
  Link2Off,
  AlertTriangle
} from 'lucide-react';

interface ExtendedUserProfile extends UserProfile {
  bio?: string;
  phoneNumber?: string;
  preferredLanguage?: string;
  timezone?: string;
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

export default function ProfilePage() {
  const { user, tokens, updateUser } = useAuth();
  const queryClient = useQueryClient();

  const fileInputRef = useRef<HTMLInputElement>(null);

  const extendedUser = user as ExtendedUserProfile | null;

  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab');
  const [activeTab, setActiveTab] = useState(tabParam || 'profile');

  // Update tab when URL param changes
  useEffect(() => {
    if (tabParam) {
      setActiveTab(tabParam);
    }
  }, [tabParam]);

  const handleTabChange = (value: string) => {
    setActiveTab(value);
    setSearchParams({ tab: value });
  };

  const [formData, setFormData] = useState<UpdateProfileRequest>({
    username: user?.username || '',
    displayName: user?.displayName || '',
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    bio: extendedUser?.bio || '',
    phoneNumber: extendedUser?.phoneNumber || '',
    preferredLanguage: extendedUser?.preferredLanguage || 'ja',
    timezone: extendedUser?.timezone || 'Asia/Tokyo',
  });

  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Email change state
  const [emailDialogOpen, setEmailDialogOpen] = useState(false);
  const [newEmail, setNewEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [emailStep, setEmailStep] = useState<'input' | 'verify'>('input');
  const [emailError, setEmailError] = useState<string | null>(null);

  // Avatar upload state
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [changeDialogOpen, setChangeDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  // Security Page State
  // Password change state
  const [passwordFormData, setPasswordFormData] = useState<UpdatePasswordRequest>({
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

  const hasGitHubLinked = extendedUser?.oauth2Provider === 'github';
  const hasPassword = extendedUser?.hasPassword !== false;

  const updateProfileMutation = useMutation({
    mutationFn: async (data: UpdateProfileRequest) => {
      if (!tokens?.accessToken) throw new Error('Not authenticated');
      return updateProfile(data);
    },
    onSuccess: (response) => {
      setMessage({ type: 'success', text: 'プロフィールを更新しました' });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      updateUser({
        username: response.username,
        displayName: response.displayName,
        firstName: response.firstName,
        lastName: response.lastName,
      });
    },
    onError: (error: Error) => {
      setMessage({ type: 'error', text: error.message });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    updateProfileMutation.mutate(formData);
  };

  // Email change handlers
  const handleRequestEmailOtp = async () => {
    setEmailError(null);
    try {
      await requestOtp({ email: newEmail, purpose: 'EMAIL_VERIFICATION' });
      setEmailStep('verify');
    } catch (error: unknown) {
      const err = error as { message?: string };
      setEmailError(err.message || 'OTPの送信に失敗しました');
    }
  };

  const handleVerifyEmailOtp = async () => {
    setEmailError(null);
    try {
      const result = await verifyOtp({ email: newEmail, otpCode, purpose: 'EMAIL_VERIFICATION' });
      if (result.data === true || (typeof result.data === 'object' && result.data)) {
        await updateEmail(newEmail);
        updateUser({ email: newEmail });
        setEmailDialogOpen(false);
        setEmailStep('input');
        setNewEmail('');
        setOtpCode('');
        setMessage({ type: 'success', text: 'メールアドレスを更新しました' });
      } else {
        setEmailError('認証コードが正しくありません');
      }
    } catch (error: unknown) {
      const err = error as { message?: string };
      setEmailError(err.message || '認証に失敗しました');
    }
  };

  // Avatar handlers
  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file
    if (!file.type.startsWith('image/')) {
      setMessage({ type: 'error', text: '画像ファイルを選択してください' });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setMessage({ type: 'error', text: 'ファイルサイズは5MB以下にしてください' });
      return;
    }

    setPendingFile(file);
    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    setChangeDialogOpen(true);
  };

  const confirmUpload = async () => {
    if (!pendingFile) return;

    setAvatarUploading(true);
    try {
      const result = await uploadAvatar(pendingFile);
      updateUser({ avatarUrl: result.avatarUrl } as Partial<UserProfile>);
      setMessage({ type: 'success', text: 'アバターを更新しました' });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      setChangeDialogOpen(false);
      setPendingFile(null);
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        setPreviewUrl(null);
      }
    } catch (error: unknown) {
      const err = error as { message?: string };
      setMessage({ type: 'error', text: err.message || 'アバターのアップロードに失敗しました' });
    } finally {
      setAvatarUploading(false);
      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const confirmDelete = async () => {
    setAvatarUploading(true);
    try {
      await deleteAvatar();
      updateUser({ avatarUrl: undefined } as Partial<UserProfile>);
      setMessage({ type: 'success', text: 'アバターを削除しました' });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      setDeleteDialogOpen(false);
    } catch (error: unknown) {
      const err = error as { message?: string };
      setMessage({ type: 'error', text: err.message || 'アバターの削除に失敗しました' });
      setDeleteDialogOpen(false);
    } finally {
      setAvatarUploading(false);
    }
  };

  // Security Handlers
  const updatePasswordMutation = useMutation({
    mutationFn: async (data: UpdatePasswordRequest) => {
      if (!tokens?.accessToken) throw new Error('Not authenticated');
      return updatePassword(data);
    },
    onSuccess: () => {
      setPasswordMessage({ type: 'success', text: 'パスワードを更新しました' });
      setPasswordFormData({ currentPassword: '', newPassword: '' });
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

    if (passwordFormData.newPassword !== confirmPassword) {
      setPasswordMessage({ type: 'error', text: '新しいパスワードが一致しません' });
      return;
    }

    if (passwordFormData.newPassword.length < 8) {
      setPasswordMessage({ type: 'error', text: 'パスワードは8文字以上にしてください' });
      return;
    }

    updatePasswordMutation.mutate(passwordFormData);
  };

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

  const githubLoginUrl = `${window.location.origin}/mapi/oauth2/authorization/github`;

  return (
    <div className="container mx-auto py-8 max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold">プロフィール設定</h1>
      </div>

      <Tabs value={activeTab} onValueChange={handleTabChange} className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="profile" className="flex items-center gap-2">
            <User className="w-4 h-4" />
            基本情報
          </TabsTrigger>
          <TabsTrigger value="account" className="flex items-center gap-2">
            <Mail className="w-4 h-4" />
            アカウント
          </TabsTrigger>
          <TabsTrigger value="security" className="flex items-center gap-2">
            <Shield className="w-4 h-4" />
            セキュリティ
          </TabsTrigger>
        </TabsList>

        {/* Basic Profile Tab */}
        <TabsContent value="profile" className="space-y-6">
          {/* Avatar Section */}
          <Card>
            <CardHeader>
              <CardTitle>プロフィール画像</CardTitle>
              <CardDescription>あなたを表すアバター画像を設定できます</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-6">
                <div className="relative group">
                  <Avatar 
                    src={extendedUser?.avatarUrl}
                    alt={user?.displayName || user?.email}
                    fallback={user?.displayName?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || 'U'}
                    size="xl"
                    className="w-24 h-24"
                  />
                  <button
                    onClick={handleAvatarClick}
                    disabled={avatarUploading}
                    className="absolute inset-0 flex items-center justify-center bg-black/50 rounded-full opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                  >
                    {avatarUploading ? (
                      <Loader2 className="w-6 h-6 text-white animate-spin" />
                    ) : (
                      <Camera className="w-6 h-6 text-white" />
                    )}
                  </button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleAvatarChange}
                    className="hidden"
                  />
                </div>
                <div className="flex flex-col gap-2">
                  <Button variant="outline" size="sm" onClick={handleAvatarClick} disabled={avatarUploading}>
                    <Camera className="w-4 h-4 mr-2" />
                    画像を変更
                  </Button>
                  {extendedUser?.avatarUrl && (
                    <Button variant="ghost" size="sm" onClick={() => setDeleteDialogOpen(true)} className="text-destructive hover:text-destructive">
                      <Trash2 className="w-4 h-4 mr-2" />
                      削除
                    </Button>
                  )}
                  <p className="text-xs text-muted-foreground">
                    JPG, PNG, GIF形式、5MB以下
                  </p>
                </div>
              </div>

              {/* Avatar Change Confirmation Dialog */}
              <Dialog open={changeDialogOpen} onOpenChange={(open) => {
                if (!open) {
                  setChangeDialogOpen(false);
                  setPendingFile(null);
                  if (previewUrl) {
                    URL.revokeObjectURL(previewUrl);
                    setPreviewUrl(null);
                  }
                  // Reset file input
                  if (fileInputRef.current) {
                    fileInputRef.current.value = '';
                  }
                }
              }}>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>プロフィール画像の変更</DialogTitle>
                    <DialogDescription>
                      この画像を新しいプロフィール画像として設定しますか？
                    </DialogDescription>
                  </DialogHeader>
                  <div className="flex justify-center py-4">
                     <Avatar 
                      src={previewUrl} 
                      alt="Preview" 
                      size="xl" 
                      className="w-32 h-32"
                    />
                  </div>
                  <DialogFooter>
                    <Button variant="outline" onClick={() => setChangeDialogOpen(false)}>
                      キャンセル
                    </Button>
                    <Button onClick={confirmUpload} disabled={avatarUploading}>
                      {avatarUploading ? '保存中...' : '保存'}
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>

              {/* Avatar Delete Confirmation Dialog */}
              <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>プロフィール画像の削除</DialogTitle>
                    <DialogDescription>
                      現在のプロフィール画像を削除し、デフォルトの表示に戻しますか？この操作は取り消せません。
                    </DialogDescription>
                  </DialogHeader>
                  <DialogFooter>
                    <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>
                      キャンセル
                    </Button>
                    <Button variant="destructive" onClick={confirmDelete} disabled={avatarUploading}>
                      {avatarUploading ? '削除中...' : '削除'}
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            </CardContent>
          </Card>

          {/* Basic Info */}
          <Card>
            <CardHeader>
              <CardTitle>基本情報</CardTitle>
              <CardDescription>プロフィールの基本情報を編集できます</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* Username */}
                <div className="space-y-2">
                  <FormLabel htmlFor="username">ユーザー名</FormLabel>
                  <Input
                    id="username"
                    type="text"
                    value={formData.username}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, username: e.target.value })}
                    placeholder="ユーザー名"
                  />
                  <p className="text-sm text-muted-foreground">@から始まる一意の識別子です</p>
                </div>

                {/* Display Name */}
                <div className="space-y-2">
                  <FormLabel htmlFor="displayName">表示名 *</FormLabel>
                  <Input
                    id="displayName"
                    type="text"
                    value={formData.displayName}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, displayName: e.target.value })}
                    required
                  />
                </div>

                {/* Name Row */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <FormLabel htmlFor="lastName">姓</FormLabel>
                    <Input
                      id="lastName"
                      type="text"
                      value={formData.lastName}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, lastName: e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <FormLabel htmlFor="firstName">名</FormLabel>
                    <Input
                      id="firstName"
                      type="text"
                      value={formData.firstName}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, firstName: e.target.value })}
                    />
                  </div>
                </div>

                {/* Bio */}
                <div className="space-y-2">
                  <FormLabel htmlFor="bio">自己紹介</FormLabel>
                  <Textarea
                    id="bio"
                    value={formData.bio}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setFormData({ ...formData, bio: e.target.value })}
                    placeholder="簡単な自己紹介を入力してください"
                    rows={3}
                  />
                </div>

                {/* Phone */}
                <div className="space-y-2">
                  <FormLabel htmlFor="phoneNumber">電話番号</FormLabel>
                  <Input
                    id="phoneNumber"
                    type="tel"
                    value={formData.phoneNumber}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, phoneNumber: e.target.value })}
                    placeholder="090-1234-5678"
                  />
                </div>

                {/* Message */}
                {message && (
                  <div
                    className={`p-3 rounded ${
                      message.type === 'success'
                        ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400'
                        : 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400'
                    }`}
                  >
                    {message.text}
                  </div>
                )}

                {/* Submit Button */}
                <Button type="submit" disabled={updateProfileMutation.isPending}>
                  {updateProfileMutation.isPending ? '更新中...' : '保存'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Account Tab */}
        <TabsContent value="account" className="space-y-6">
          {/* Email Section */}
          <Card>
            <CardHeader>
              <CardTitle>メールアドレス</CardTitle>
              <CardDescription>アカウントに紐づくメールアドレスを管理します</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-muted/50 rounded-lg">
                <div className="flex items-center gap-3">
                  <Mail className="w-5 h-5 text-muted-foreground" />
                  <div>
                    <p className="font-medium">{user?.email}</p>
                    <p className="text-sm text-muted-foreground">
                      {user?.emailVerified ? (
                        <span className="flex items-center gap-1 text-green-600 dark:text-green-400">
                          <Check className="w-3 h-3" /> 検証済み
                        </span>
                      ) : (
                        <span className="text-yellow-600 dark:text-yellow-400">未検証</span>
                      )}
                    </p>
                  </div>
                </div>
                <Button variant="outline" size="sm" onClick={() => setEmailDialogOpen(true)}>
                  変更
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Regional Settings */}
          <Card>
            <CardHeader>
              <CardTitle>地域設定</CardTitle>
              <CardDescription>言語とタイムゾーンの設定</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <FormLabel htmlFor="preferredLanguage">言語</FormLabel>
                    <select
                      id="preferredLanguage"
                      value={formData.preferredLanguage}
                      onChange={(e) => setFormData({ ...formData, preferredLanguage: e.target.value })}
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                      <option value="ja">日本語</option>
                      <option value="en">English</option>
                    </select>
                  </div>
                  <div className="space-y-2">
                    <FormLabel htmlFor="timezone">タイムゾーン</FormLabel>
                    <select
                      id="timezone"
                      value={formData.timezone}
                      onChange={(e) => setFormData({ ...formData, timezone: e.target.value })}
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                      <option value="Asia/Tokyo">日本標準時 (JST)</option>
                      <option value="UTC">協定世界時 (UTC)</option>
                      <option value="America/New_York">東部標準時 (EST)</option>
                      <option value="Europe/London">グリニッジ標準時 (GMT)</option>
                    </select>
                  </div>
                </div>
                <Button type="submit" disabled={updateProfileMutation.isPending}>
                  {updateProfileMutation.isPending ? '更新中...' : '保存'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Security Tab */}
        <TabsContent value="security" className="space-y-6">
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
                      value={passwordFormData.currentPassword}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPasswordFormData({ ...passwordFormData, currentPassword: e.target.value })}
                      required
                      autoComplete="current-password"
                    />
                  </div>

                  <div className="space-y-2">
                    <FormLabel htmlFor="newPassword">新しいパスワード *</FormLabel>
                    <Input
                      id="newPassword"
                      type="password"
                      value={passwordFormData.newPassword}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPasswordFormData({ ...passwordFormData, newPassword: e.target.value })}
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
        </TabsContent>
      </Tabs>

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

      {/* Email Change Dialog */}
      <Dialog open={emailDialogOpen} onOpenChange={setEmailDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>メールアドレスの変更</DialogTitle>
            <DialogDescription>
              {emailStep === 'input' 
                ? '新しいメールアドレスを入力してください。認証コードが送信されます。'
                : `${newEmail} に送信された認証コードを入力してください。`
              }
            </DialogDescription>
          </DialogHeader>
          
          {emailStep === 'input' ? (
            <div className="space-y-4">
              <div className="space-y-2">
                <FormLabel htmlFor="newEmail">新しいメールアドレス</FormLabel>
                <Input
                  id="newEmail"
                  type="email"
                  value={newEmail}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNewEmail(e.target.value)}
                  placeholder="new@example.com"
                />
              </div>
              {emailError && (
                <p className="text-sm text-red-600 dark:text-red-400">{emailError}</p>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="space-y-2">
                <FormLabel htmlFor="otpCode">認証コード</FormLabel>
                <Input
                  id="otpCode"
                  type="text"
                  value={otpCode}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setOtpCode(e.target.value)}
                  placeholder="6桁の認証コード"
                  maxLength={6}
                />
              </div>
              {emailError && (
                <p className="text-sm text-red-600 dark:text-red-400">{emailError}</p>
              )}
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => {
              setEmailDialogOpen(false);
              setEmailStep('input');
              setNewEmail('');
              setOtpCode('');
              setEmailError(null);
            }}>
              キャンセル
            </Button>
            {emailStep === 'input' ? (
              <Button onClick={handleRequestEmailOtp} disabled={!newEmail}>
                認証コードを送信
              </Button>
            ) : (
              <Button onClick={handleVerifyEmailOtp} disabled={otpCode.length !== 6}>
                確認
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
