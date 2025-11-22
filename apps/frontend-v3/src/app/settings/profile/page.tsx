import { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@mirel/ui';
import { Input } from '@mirel/ui';
import { FormLabel } from '@mirel/ui';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@mirel/ui';
import { updateProfile, type UpdateProfileRequest } from '@/lib/api/userProfile';
import { useQueryClient, useMutation } from '@tanstack/react-query';

export default function ProfilePage() {
  const { user, tokens, updateUser } = useAuth();
  const queryClient = useQueryClient();

  const [formData, setFormData] = useState<UpdateProfileRequest>({
    displayName: user?.displayName || '',
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
  });

  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const updateProfileMutation = useMutation({
    mutationFn: async (data: UpdateProfileRequest) => {
      if (!tokens?.accessToken) throw new Error('Not authenticated');
      return updateProfile(tokens.accessToken, data);
    },
    onSuccess: (response) => {
      setMessage({ type: 'success', text: 'プロフィールを更新しました' });
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      // authStore の user を更新してメニュー等に即座に反映
      updateUser({
        displayName: response.data.displayName,
        firstName: response.data.firstName,
        lastName: response.data.lastName,
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

  return (
    <div className="container mx-auto py-8 max-w-2xl">
      <h1 className="text-3xl font-bold mb-6">プロフィール設定</h1>

      <Card>
        <CardHeader>
          <CardTitle>ユーザー情報</CardTitle>
          <CardDescription>あなたのプロフィール情報を編集できます</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Email (Read-only) */}
            <div className="space-y-2">
              <FormLabel htmlFor="email">メールアドレス</FormLabel>
              <Input
                id="email"
                type="email"
                value={user?.email || ''}
                disabled
                className="bg-gray-50"
              />
              <p className="text-sm text-gray-500">メールアドレスは変更できません</p>
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

            {/* First Name */}
            <div className="space-y-2">
              <FormLabel htmlFor="firstName">名</FormLabel>
              <Input
                id="firstName"
                type="text"
                value={formData.firstName}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, firstName: e.target.value })}
              />
            </div>

            {/* Last Name */}
            <div className="space-y-2">
              <FormLabel htmlFor="lastName">姓</FormLabel>
              <Input
                id="lastName"
                type="text"
                value={formData.lastName}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, lastName: e.target.value })}
              />
            </div>

            {/* Message */}
            {message && (
              <div
                className={`p-3 rounded ${
                  message.type === 'success'
                    ? 'bg-green-50 text-green-700'
                    : 'bg-red-50 text-red-700'
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
    </div>
  );
}
