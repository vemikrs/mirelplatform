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
  SignupPage, 
  PasswordResetRequestPage, 
  PasswordResetConfirmPage,
} from '@/features/auth';
import { UnifiedLoginPage } from '@/features/auth/pages/UnifiedLoginPage';
import { OtpVerifyPage } from '@/features/auth/pages/OtpVerifyPage';
import { OtpPasswordResetPage } from '@/features/auth/pages/OtpPasswordResetPage';
import { OtpPasswordResetVerifyPage } from '@/features/auth/pages/OtpPasswordResetVerifyPage';
import { OtpEmailVerificationPage } from '@/features/auth/pages/OtpEmailVerificationPage';
import { OAuthCallbackPage } from '@/features/auth/pages/OAuthCallbackPage';
import { AdminFeaturesPage } from '@/features/admin';
import { ProtectedRoute } from '@/components/auth';
import { ForbiddenPage, NotFoundPage, InternalServerErrorPage } from '@/features/error';
import { loadNavigationConfig } from './navigation.schema';
import ProfilePage from '@/app/settings/profile/page';
import SecurityPage from '@/app/settings/security/page';
import { SchemaHomePage } from '@/features/schema/pages/SchemaHomePage';
import { SchemaRecordListPage } from '@/features/schema/pages/SchemaRecordListPage';
import { SchemaRecordDetailPage } from '@/features/schema/pages/SchemaRecordDetailPage';
import { SchemaModelDefinePage } from '@/features/schema/pages/SchemaModelDefinePage';
import { SchemaCodeMasterPage } from '@/features/schema/pages/SchemaCodeMasterPage';
import { StudioPage } from '@/features/studio/pages/StudioPage';
import { StudioHomePage } from '@/features/studio/pages/StudioHomePage';
import { StudioDataListPage } from '@/features/studio/pages/StudioDataListPage';
import { StudioDataEditPage } from '@/features/studio/pages/StudioDataEditPage';
import { useAuthStore } from '@/stores/authStore';
import axios from 'axios';
import { TitleUpdater } from '@/components/TitleUpdater';
import type { NavigationConfig } from './navigation.schema';

// キャッシュ用の変数（同一セッション内での重複API呼び出しを防ぐ）
let cachedData: { profile: unknown; navigation: NavigationConfig } | null = null;
let cacheKey = '';
let cacheTimestamp = 0;
const CACHE_DURATION = 5000; // 5秒

/** * Clear authentication loader cache
 * Should be called on logout to ensure fresh authentication check on next navigation
 */
export function clearAuthLoaderCache() {
  cachedData = null;
  cacheKey = '';
  cacheTimestamp = 0;
  console.log('[authLoader] Cache cleared');
}

/** * Authentication Loader
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
  {
    element: (
      <>
        <TitleUpdater />
        <Outlet />
      </>
    ),
    children: [
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
    element: <UnifiedLoginPage />,
    handle: { title: 'ログイン' },
  },
  {
    path: '/signup',
    element: <SignupPage />,
    handle: { title: '新規登録' },
  },
  {
    path: '/password-reset',
    element: <PasswordResetRequestPage />,
    handle: { title: 'パスワードリセット' },
  },
  {
    path: '/password-reset/confirm',
    element: <PasswordResetConfirmPage />,
    handle: { title: 'パスワードリセット確認' },
  },
  // OTP Authentication Routes
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
    handle: { title: 'ホーム' },
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
        handle: { title: 'UIカタログ' },
      },
      {
        path: 'sitemap',
        element: <SiteMapPage />,
        handle: { title: 'サイトマップ' },
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
            handle: { title: 'ホーム' },
          },
          {
            index: true,
            loader: () => redirect('/home'),
          },
          {
            path: 'promarker',
            element: <ProMarkerPageWithErrorBoundary />,
            handle: { title: 'ProMarker - 払出画面' },
          },
          {
            path: 'promarker/stencils',
            element: <StencilListPage />,
            handle: { title: 'ProMarker - ステンシル一覧' },
          },
          {
            path: 'promarker/editor/*',
            element: <StencilEditor />,
            handle: { title: 'ProMarker - エディタ' },
          },
          {
            path: 'settings/profile',
            element: <ProfilePage />,
          },
          {
            path: 'settings/security',
            element: <SecurityPage />,
          },
          // Admin routes (requires ADMIN role)
          {
            path: 'admin/features',
            element: <AdminFeaturesPage />,
            handle: { title: '管理 - フィーチャーフラグ' },
          },
          // Backward compatibility route for E2E tests
          {
            path: 'mirel/mste',
            element: <ProMarkerPageWithErrorBoundary />,
          },
        ],
      },
      {
        path: 'apps/schema',
        children: [
          {
            index: true,
            element: <SchemaHomePage />,
            handle: { title: 'Schema - Home' },
          },
          {
            path: 'models',
            element: <SchemaModelDefinePage />,
            handle: { title: 'Schema - Model Definition' },
          },
          {
            path: 'records',
            element: <SchemaRecordListPage />,
            handle: { title: 'Schema - Records' },
          },
          {
            path: 'records/:modelId/new',
            element: <SchemaRecordDetailPage />,
            handle: { title: 'Schema - New Record' },
          },
          {
            path: 'records/:modelId/:recordId',
            element: <SchemaRecordDetailPage />,
            handle: { title: 'Schema - Edit Record' },
          },
          {
            path: 'codes',
            element: <SchemaCodeMasterPage />,
            handle: { title: 'Schema - Code Master' },
          },
        ],
      },
      {
        path: 'apps/studio',
        children: [
          {
            index: true,
            element: <StudioHomePage />,
            handle: { title: 'Studio - Home' },
          },
          {
            path: 'new',
            element: <StudioPage />,
            handle: { title: 'Studio - New Form' },
          },
          {
            path: ':modelId',
            element: <StudioPage />,
            handle: { title: 'Studio - Edit Form' },
          },
          {
            path: ':modelId/data',
            element: <StudioDataListPage />,
            handle: { title: 'Studio - Data List' },
          },
          {
            path: ':modelId/data/:recordId',
            element: <StudioDataEditPage />,
            handle: { title: 'Studio - Edit Data' },
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
    ]
  }
]);
