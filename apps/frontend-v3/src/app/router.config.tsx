import { createBrowserRouter, Outlet, redirect } from 'react-router-dom';
import { RootLayout } from '@/layouts/RootLayout';
import { HomePage } from '@/features/home/pages/HomePage';
import { UiCatalogPage } from '@/features/catalog/pages/UiCatalogPage';
import { SiteMapPage } from '@/features/sitemap/pages/SiteMapPage';
import ProMarkerPageWithErrorBoundary from '@/features/promarker/pages/ProMarkerPage';
import { StencilEditor } from '@/features/stencil-editor/components/StencilEditor';
import { StencilListPage } from '@/features/stencil-editor/pages/StencilListPage';
import { SaaSStatusPage } from '@/features/saas-status';
import { 
  LoginPage, 
  SignupPage, 
  PasswordResetRequestPage, 
  PasswordResetConfirmPage,
} from '@/features/auth';
import { OtpLoginPage } from '@/features/auth/pages/OtpLoginPage';
import { OtpVerifyPage } from '@/features/auth/pages/OtpVerifyPage';
import { OtpPasswordResetPage } from '@/features/auth/pages/OtpPasswordResetPage';
import { OtpPasswordResetVerifyPage } from '@/features/auth/pages/OtpPasswordResetVerifyPage';
import { OtpEmailVerificationPage } from '@/features/auth/pages/OtpEmailVerificationPage';
import { OAuthCallbackPage } from '@/features/auth/pages/OAuthCallbackPage';
import { ProtectedRoute } from '@/components/auth';
import { ForbiddenPage, NotFoundPage, InternalServerErrorPage } from '@/features/error';
import { loadNavigationConfig } from './navigation.schema';
import ProfilePage from '@/app/settings/profile/page';
import SecurityPage from '@/app/settings/security/page';
import { useAuthStore } from '@/stores/authStore';
import axios from 'axios';
import type { NavigationConfig } from './navigation.schema';

// キャッシュ用の変数（同一セッション内での重複API呼び出しを防ぐ）
let cachedData: { profile: unknown; navigation: NavigationConfig } | null = null;
let cacheKey = '';
let cacheTimestamp = 0;
const CACHE_DURATION = 5000; // 5秒

/**
 * Authentication Loader
 * - HttpOnly Cookie を前提とし、サーバセッション (/users/me 系) を単一の真実とする
 * - 成功時は authStore.rehydrateFromServerSession() がストアを完全に再構築
 */
async function authLoader(): Promise<NavigationConfig> {
  try {
    const now = Date.now();
    const currentKey = window.location.pathname;
    
    // キャッシュが有効かつ同じURLならスキップ
    if (cachedData && cacheKey === currentKey && (now - cacheTimestamp) < CACHE_DURATION) {
      return cachedData.navigation;
    }
    
    // サーバセッションから authStore を再構築
    const { rehydrateFromServerSession } = useAuthStore.getState();
    await rehydrateFromServerSession();

    const navigation = await loadNavigationConfig();
    
    cachedData = { profile: null, navigation };
    cacheKey = currentKey;
    cacheTimestamp = now;
    
    return navigation;
  } catch (error) {
    // 401の場合、インターセプターで既にログアウト&リダイレクト済み
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        throw redirect('/login');
      }
      // ネットワークエラー（ERR_NETWORK, ERR_TOO_MANY_REDIRECTS等）の場合もログイン画面へ
      // セッション確立失敗とみなす
      if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
        console.warn('[authLoader] Network Error detected, redirecting to login', error);
        throw redirect('/login');
      }
    }
    throw error;
  }
}

/**
 * React Router v7 configuration
 * Defines application routes and navigation structure
 */
export const router = createBrowserRouter([
  // Error Pages (static routes)
  {
    path: '/403',
    element: <ForbiddenPage />,
  },
  {
    path: '/500',
    element: <InternalServerErrorPage />,
  },
  // Auth Routes (no authentication required)
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/signup',
    element: <SignupPage />,
  },
  {
    path: '/password-reset',
    element: <PasswordResetRequestPage />,
  },
  {
    path: '/password-reset/confirm',
    element: <PasswordResetConfirmPage />,
  },
  // OTP Authentication Routes
  {
    path: '/auth/otp-login',
    element: <OtpLoginPage />,
  },
  {
    path: '/auth/otp-verify',
    element: <OtpVerifyPage />,
  },
  {
    path: '/auth/password-reset',
    element: <OtpPasswordResetPage />,
  },
  {
    path: '/auth/password-reset-verify',
    element: <OtpPasswordResetVerifyPage />,
  },
  {
    path: '/auth/email-verification',
    element: <OtpEmailVerificationPage />,
  },
  // OAuth2 Callback Route
  {
    path: '/auth/oauth2/success',
    element: <OAuthCallbackPage />,
  },
  // App Root with authentication
  {
    id: 'app-root',
    path: '/',
    element: <RootLayout />,
    loader: authLoader,
    errorElement: <InternalServerErrorPage />,
    children: [
      {
        path: 'saas-status',
        element: <SaaSStatusPage />,
      },
      {
        path: 'catalog',
        element: <UiCatalogPage />,
      },
      {
        path: 'sitemap',
        element: <SiteMapPage />,
      },
      {
        element: (
          <ProtectedRoute>
            <Outlet />
          </ProtectedRoute>
        ),
        children: [
          {
            path: 'home',
            element: <HomePage />,
          },
          {
            index: true,
            loader: () => redirect('/home'),
          },
          {
            path: 'promarker',
            element: <ProMarkerPageWithErrorBoundary />,
          },
          {
            path: 'promarker/stencils',
            element: <StencilListPage />,
          },
          {
            path: 'promarker/editor/*',
            element: <StencilEditor />,
          },
          {
            path: 'settings/profile',
            element: <ProfilePage />,
          },
          {
            path: 'settings/security',
            element: <SecurityPage />,
          },
          // Backward compatibility route for E2E tests
          {
            path: 'mirel/mste',
            element: <ProMarkerPageWithErrorBoundary />,
          },
        ],
      },
    ],
  },
  // 404 Catch-all (must be last, outside RootLayout to avoid authLoader)
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
