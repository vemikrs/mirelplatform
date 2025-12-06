/*
 * Copyright(c) 2025 mirelplatform.
 */
import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/lib/hooks/useTheme';
import { Button, Card, Input } from '@mirel/ui';
import axios from 'axios';

export function SignupPage() {
  // テーマを初期化
  useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const signup = useAuthStore((state) => state.signup);
  const setAuth = useAuthStore((state) => state.setAuth);

  const [isOAuth2, setIsOAuth2] = useState(false);
  const [oauthToken, setOauthToken] = useState('');

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    displayName: '',
    firstName: '',
    lastName: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (location.state?.oauth2 && location.state?.token) {
      setIsOAuth2(true);
      setOauthToken(location.state.token);
      // GitHubから取得できる情報があればプレフィルしたいが、
      // 現状トークンしかないので、ユーザーに手動入力してもらう
      // (本来はトークンを使って /auth/me 的なAPIでSystemUser情報を取れると良い)
    }
  }, [location]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!isOAuth2) {
      // 通常のバリデーション
      if (formData.password !== formData.confirmPassword) {
        setError('パスワードが一致しません。');
        return;
      }
      if (formData.password.length < 8) {
        setError('パスワードは8文字以上で入力してください。');
        return;
      }
    }

    setLoading(true);

    try {
      if (isOAuth2) {
        // OAuth2サインアップ
        // apiClientを使わずaxios直接呼び出し（トークンヘッダー付与のため）
        const response = await axios.post('/mapi/auth/signup/oauth2', {
          username: formData.username,
          displayName: formData.displayName,
          firstName: formData.firstName,
          lastName: formData.lastName,
          // emailはSystemUserのものを使うので送信不要だが、フォームにあるなら送っても無視されるかエラーになるか
          // バックエンド実装ではRequest Bodyにemailフィールドがないので送らなくてOK
        }, {
          headers: {
            'Authorization': `Bearer ${oauthToken}`,
            'Content-Type': 'application/json'
          }
        });

        // 成功したらログイン状態にしてホームへ
        const data = response.data;
        setAuth(data.user, data.currentTenant, data.tokens);
        navigate('/home');

      } else {
        // 通常サインアップ
        await signup({
          username: formData.username,
          email: formData.email,
          password: formData.password,
          displayName: formData.displayName,
          firstName: formData.firstName,
          lastName: formData.lastName,
        });
        navigate('/');
      }
    } catch (err: any) {
      console.error('Signup failed:', err);
      if (err.response?.data?.message) {
        setError(`サインアップに失敗しました: ${err.response.data.message}`);
      } else {
        setError('サインアップに失敗しました。入力内容を確認してください。');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <Card className="w-full max-w-md p-8">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold mb-2 text-foreground">mirelplatform</h1>
          <p className="text-muted-foreground">
            {isOAuth2 ? 'アカウント情報の登録' : '新規アカウント作成'}
          </p>
          {isOAuth2 && (
            <p className="text-sm text-blue-600 mt-2">
              GitHub認証が完了しました。<br/>続けてアカウント情報を入力してください。
            </p>
          )}
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-medium mb-2 text-foreground">
              ユーザー名 *
            </label>
            <Input
              id="username"
              type="text"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              required
              placeholder="username"
              className="w-full"
              minLength={3}
              maxLength={50}
            />
            <p className="text-xs text-gray-500 mt-1">3〜50文字で入力してください</p>
          </div>

          {!isOAuth2 && (
            <div>
              <label htmlFor="email" className="block text-sm font-medium mb-2 text-foreground">
                メールアドレス *
              </label>
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                required
                placeholder="user@example.com"
                className="w-full"
              />
            </div>
          )}

          <div>
            <label htmlFor="displayName" className="block text-sm font-medium mb-2 text-foreground">
              表示名 *
            </label>
            <Input
              id="displayName"
              type="text"
              value={formData.displayName}
              onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
              required
              placeholder="山田 太郎"
              className="w-full"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium mb-2 text-foreground">
                名
              </label>
              <Input
                id="firstName"
                type="text"
                value={formData.firstName}
                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                placeholder="太郎"
                className="w-full"
              />
            </div>
            <div>
              <label htmlFor="lastName" className="block text-sm font-medium mb-2 text-foreground">
                姓
              </label>
              <Input
                id="lastName"
                type="text"
                value={formData.lastName}
                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                placeholder="山田"
                className="w-full"
              />
            </div>
          </div>

          {!isOAuth2 && (
            <>
              <div>
                <label htmlFor="password" className="block text-sm font-medium mb-2 text-foreground">
                  パスワード *
                </label>
                <Input
                  id="password"
                  type="password"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  required
                  placeholder="••••••••"
                  className="w-full"
                />
                <p className="text-xs text-gray-500 mt-1">8文字以上で入力してください</p>
              </div>

              <div>
                <label htmlFor="confirmPassword" className="block text-sm font-medium mb-2 text-foreground">
                  パスワード（確認） *
                </label>
                <Input
                  id="confirmPassword"
                  type="password"
                  value={formData.confirmPassword}
                  onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                  required
                  placeholder="••••••••"
                  className="w-full"
                />
              </div>
            </>
          )}

          {error && (
            <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm">
              {error}
            </div>
          )}

          <Button
            type="submit"
            disabled={loading}
            className="w-full"
          >
            {loading ? '処理中...' : (isOAuth2 ? '登録して開始' : 'アカウントを作成')}
          </Button>
        </form>

        {!isOAuth2 && (
          <div className="mt-6 text-center text-sm text-muted-foreground">
            <p>すでにアカウントをお持ちの方は、</p>
            <p className="mt-1">
              <a href="/login" className="text-primary hover:underline">
                ログイン
              </a>
            </p>
          </div>
        )}
      </Card>
    </div>
  );
}
