import { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@mirel/ui';
import { Input } from '@mirel/ui';
import { FormLabel } from '@mirel/ui';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@mirel/ui';
import { updatePassword, type UpdatePasswordRequest } from '@/lib/api/userProfile';
import { useMutation } from '@tanstack/react-query';

export default function SecurityPage() {
  const { tokens } = useAuth();

  const [formData, setFormData] = useState<UpdatePasswordRequest>({
    currentPassword: '',
    newPassword: '',
  });
  const [confirmPassword, setConfirmPassword] = useState('');

  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const updatePasswordMutation = useMutation({
    mutationFn: async (data: UpdatePasswordRequest) => {
      if (!tokens?.accessToken) throw new Error('Not authenticated');
      return updatePassword(tokens.accessToken, data);
    },
    onSuccess: () => {
      setMessage({ type: 'success', text: 'パスワードを更新しました' });
      setFormData({ currentPassword: '', newPassword: '' });
      setConfirmPassword('');
    },
    onError: (error: Error) => {
      setMessage({ type: 'error', text: error.message });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);

    // Validate password match
    if (formData.newPassword !== confirmPassword) {
      setMessage({ type: 'error', text: '新しいパスワードが一致しません' });
      return;
    }

    // Validate password length
    if (formData.newPassword.length < 8) {
      setMessage({ type: 'error', text: 'パスワードは8文字以上にしてください' });
      return;
    }

    updatePasswordMutation.mutate(formData);
  };

  return (
    <div className="container mx-auto py-8 max-w-2xl">
      <h1 className="text-3xl font-bold mb-6">セキュリティ設定</h1>

      <Card>
        <CardHeader>
          <CardTitle>パスワード変更</CardTitle>
          <CardDescription>アカウントのパスワードを変更できます</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Current Password */}
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

            {/* New Password */}
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
              <p className="text-sm text-gray-500">8文字以上で入力してください</p>
            </div>

            {/* Confirm Password */}
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
            <Button type="submit" disabled={updatePasswordMutation.isPending}>
              {updatePasswordMutation.isPending ? '更新中...' : 'パスワードを変更'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
