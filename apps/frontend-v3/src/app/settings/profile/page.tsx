import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
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
  type UpdateProfileRequest,
  type UserProfile
} from '@/lib/api/userProfile';
import { requestOtp, verifyOtp } from '@/lib/api/otp';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { User, Mail, Shield, Camera, Trash2, Check, Loader2 } from 'lucide-react';

interface ExtendedUserProfile extends UserProfile {
  bio?: string;
  phoneNumber?: string;
  preferredLanguage?: string;
  timezone?: string;
}

export default function ProfilePage() {
  const { user, tokens, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const extendedUser = user as ExtendedUserProfile | null;

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

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
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

    setAvatarUploading(true);
    try {
      const result = await uploadAvatar(file);
      updateUser({ avatarUrl: result.avatarUrl } as Partial<UserProfile>);
      setMessage({ type: 'success', text: 'アバターを更新しました' });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    } catch (error: unknown) {
      const err = error as { message?: string };
      setMessage({ type: 'error', text: err.message || 'アバターのアップロードに失敗しました' });
    } finally {
      setAvatarUploading(false);
    }
  };

  const handleDeleteAvatar = async () => {
    try {
      await deleteAvatar();
      updateUser({ avatarUrl: undefined } as Partial<UserProfile>);
      setMessage({ type: 'success', text: 'アバターを削除しました' });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
    } catch (error: unknown) {
      const err = error as { message?: string };
      setMessage({ type: 'error', text: err.message || 'アバターの削除に失敗しました' });
    }
  };

  return (
    <div className="container mx-auto py-8 max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold">プロフィール設定</h1>
        <Button variant="outline" onClick={() => navigate('/settings/security')}>
          <Shield className="w-4 h-4 mr-2" />
          セキュリティ設定
        </Button>
      </div>

      <Tabs defaultValue="profile" className="space-y-6">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="profile" className="flex items-center gap-2">
            <User className="w-4 h-4" />
            基本情報
          </TabsTrigger>
          <TabsTrigger value="account" className="flex items-center gap-2">
            <Mail className="w-4 h-4" />
            アカウント
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
                    <Button variant="ghost" size="sm" onClick={handleDeleteAvatar} className="text-destructive hover:text-destructive">
                      <Trash2 className="w-4 h-4 mr-2" />
                      削除
                    </Button>
                  )}
                  <p className="text-xs text-muted-foreground">
                    JPG, PNG, GIF形式、5MB以下
                  </p>
                </div>
              </div>
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
      </Tabs>

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
