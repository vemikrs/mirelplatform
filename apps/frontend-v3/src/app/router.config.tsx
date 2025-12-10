import { createBrowserRouter, Outlet, redirect } from 'react-router-dom';
import { RootLayout } from '@/layouts/RootLayout';
import { HomePage } from '@/features/home/pages/HomePage';
import { ProductLineupPage } from '@/features/home/pages/ProductLineupPage';
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
  SetupAccountPage,
} from '@/features/auth';
import { UnifiedLoginPage } from '@/features/auth/pages/UnifiedLoginPage';
import { OtpVerifyPage } from '@/features/auth/pages/OtpVerifyPage';
import { OtpPasswordResetPage } from '@/features/auth/pages/OtpPasswordResetPage';
import { OtpPasswordResetVerifyPage } from '@/features/auth/pages/OtpPasswordResetVerifyPage';
import { OtpEmailVerificationPage } from '@/features/auth/pages/OtpEmailVerificationPage';
import { MagicVerifyPage } from '@/features/auth/pages/MagicVerifyPage';
import { OAuthCallbackPage } from '@/features/auth/pages/OAuthCallbackPage';
import { LogoutPage } from '@/features/auth/pages/LogoutPage';
import { AdminFeaturesPage } from '@/features/admin';
import { MiraAdminPage } from '@/features/admin/pages/MiraAdminPage'; // Add import
import { MenuManagementPage } from '@/features/admin/pages/MenuManagementPage';
import { UserManagementPage } from '@/features/admin/pages/UserManagementPage';
import { LicenseManagementPage } from '@/features/admin/pages/LicenseManagementPage';
import { SystemSettingsPage } from '@/features/admin/pages/SystemSettingsPage';
import { SystemStatusPage } from '@/features/admin/pages/SystemStatusPage';
import { TenantManagementPage } from '@/features/admin/pages/TenantManagementPage';
import { OrganizationManagementPage } from '@/features/organization';
import AnnouncementListPage from '@/features/admin/pages/AnnouncementListPage';
import AnnouncementEditPage from '@/features/admin/pages/AnnouncementEditPage';
import { ProtectedRoute } from '@/components/auth';
import { ForbiddenPage, NotFoundPage, InternalServerErrorPage } from '@/features/error';
import { loadNavigationConfig } from './navigation.schema';
import ProfilePage from '@/app/settings/profile/page';
import { ModelerCodeMasterPage } from '@/features/studio/modeler/pages/ModelerCodeMasterPage';
import { ModelerHomePage } from '@/features/studio/modeler/pages/ModelerHomePage';
import { StudioHomePage } from '@/features/studio/pages/StudioHomePage';
import { EntityListPage } from '@/features/studio/modeler/pages/EntityListPage';
import { EntityEditPage } from '@/features/studio/modeler/pages/EntityEditPage';
import { RelationViewPage } from '@/features/studio/modeler/pages/RelationViewPage';
import { FormListPage } from '@/features/studio/forms/pages/FormListPage';
import { FormDesignerPage } from '@/features/studio/forms/pages/FormDesignerPage';
import { FlowListPage } from '@/features/studio/flows/pages/FlowListPage';
import { FlowDesignerPage } from '@/features/studio/flows/pages/FlowDesignerPage';
import { DataBrowserPage } from '@/features/studio/data/pages/DataBrowserPage';
import { DataRecordPage } from '@/features/studio/data/pages/DataRecordPage';
import { ReleasePage } from '@/features/studio/pages/ReleasePage';
import { StudioGuard } from '@/features/studio/guards/StudioGuard';
import { MiraPage } from '@/features/mira/pages/MiraPage';
import { useAuthStore } from '@/stores/authStore';
import axios from 'axios';
import { TitleUpdater } from '@/components/TitleUpdater';
import type { NavigationConfig } from './navigation.schema';

// キャッシュ変数を削除
// let cachedData: ... (removed)
// let cacheKey = ... (removed)
// let cacheTimestamp = ... (removed)
// const CACHE_DURATION = ... (removed)

/** * Clear authentication loader cache
 * Should be called on logout to ensure fresh authentication check on next navigation
 * (No-op now as cache is removed)
 */
export function clearAuthLoaderCache() {
  // console.log('[authLoader] Cache cleared (no-op)');
}


async function authLoader({ request }: { request: Request }): Promise<NavigationConfig> {
  try {
    // キャッシュロジックを削除: 常に最新の認証状態を確認する
    
    // サーバセッションから authStore を再構築
    const { rehydrateFromServerSession } = useAuthStore.getState();
    await rehydrateFromServerSession();

    const navigation = await loadNavigationConfig();
    
    return navigation;
  } catch (error) {
    // redirect先のURLを構築 (returnUrlパラメータ)
    const url = new URL(request.url);
    const returnUrl = url.pathname + url.search;
    const loginUrl = `/login?returnUrl=${encodeURIComponent(returnUrl)}`;

    // 401の場合、インターセプターで既にログアウト&リダイレクト済み
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        throw redirect(loginUrl);
      }
      // ネットワークエラー（ERR_NETWORK, ERR_TOO_MANY_REDIRECTS等）の場合もログイン画面へ
      // セッション確立失敗とみなす
      if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
        console.warn('[authLoader] Network Error detected, redirecting to login', error);
        throw redirect(loginUrl);
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
    path: '/auth/magic-verify',
    element: <MagicVerifyPage />,
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
  {
    path: '/auth/setup-account',
    element: <SetupAccountPage />,
  },
  // OAuth2 Callback Route
  {
    path: '/auth/oauth2/success',
    element: <OAuthCallbackPage />,
  },
  {
    path: '/logout',
    element: <LogoutPage />,
    handle: { title: 'ログアウト' },
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
            path: 'products',
            element: <ProductLineupPage />,
            handle: { title: '製品ラインナップ' },
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

          // Admin routes - Platform Management (requires ADMIN role)
          {
            path: 'admin/platform/tenants',
            element: <TenantManagementPage />,
            handle: { title: '管理 - テナント一覧' },
          },
          {
            path: 'admin/platform/system',
            element: <SystemSettingsPage />,
            handle: { title: '管理 - システム共通設定' },
          },
          {
            path: 'admin/platform/status',
            element: <SystemStatusPage />,
            handle: { title: '管理 - システムステータス' },
          },
          {
            path: 'admin/platform/menu',
            element: <MenuManagementPage />,
            handle: { title: '管理 - メニュー定義' },
          },
          {
            path: 'admin/platform/sitemap',
            element: <SiteMapPage />,
            handle: { title: '管理 - サイトマップ' },
          },
          {
            path: 'admin/features',
            element: <AdminFeaturesPage />,
            handle: { title: '管理 - フィーチャーフラグ' },
          },
          // Mira AI Admin
          {
            path: 'admin/mira',
            element: <MiraAdminPage />,
            handle: { title: '管理 - Mira AI 設定' },
          },
          // Admin routes - Workspace/Tenant Management (requires TENANT_ADMIN role)
          {
            path: 'admin/workspace/license',
            element: <LicenseManagementPage />,
            handle: { title: '管理 - 契約・ライセンス管理' },
          },
          {
            path: 'admin/workspace/announcements',
            element: <AnnouncementListPage />,
            handle: { title: '管理 - 通知設定' },
          },
          {
            path: 'admin/workspace/announcements/:id',
            element: <AnnouncementEditPage />,
            handle: { title: '管理 - お知らせ編集' },
          },
          // Admin routes - Identity Management (requires TENANT_ADMIN role)
          {
            path: 'admin/identity/users',
            element: <UserManagementPage />,
            handle: { title: '管理 - ユーザー管理' },
          },
          // Admin routes - Master Maintenance (requires TENANT_ADMIN role)
          {
            path: 'admin/master/organization',
            element: <OrganizationManagementPage />,
            handle: { title: '管理 - 組織マスタ' },
          },
          {
            path: 'admin/master/codes',
            element: <ModelerCodeMasterPage />,
            handle: { title: '管理 - コードマスタ' },
          },
          // Legacy redirects for backward compatibility
          {
            path: 'admin/tenant',
            loader: () => redirect('/admin/platform/tenants'),
          },
          {
            path: 'admin/system',
            loader: () => redirect('/admin/platform/system'),
          },
          {
            path: 'admin/status',
            loader: () => redirect('/admin/platform/status'),
          },
          {
            path: 'admin/menu',
            loader: () => redirect('/admin/platform/menu'),
          },
          {
            path: 'admin/license',
            loader: () => redirect('/admin/workspace/license'),
          },
          {
            path: 'admin/announcements',
            loader: () => redirect('/admin/workspace/announcements'),
          },
          {
            path: 'admin/users',
            loader: () => redirect('/admin/identity/users'),
          },
          {
            path: 'admin/organization',
            loader: () => redirect('/admin/master/organization'),
          },
          {
            path: 'sitemap',
            loader: () => redirect('/admin/platform/sitemap'),
          },
          // Backward compatibility route for E2E tests
          {
            path: 'mirel/mste',
            element: <ProMarkerPageWithErrorBoundary />,
          },
          // Mira AI Assistant - Dedicated Page
          {
            path: 'mira',
            element: <MiraPage />,
            handle: { title: 'Mira - AI Assistant' },
          },
        ],
      },
      {
        path: 'apps/studio',
        element: (
          <>
            <StudioGuard />
          </>
        ),
        children: [
          {
            index: true,
            element: <StudioHomePage />,
            handle: { title: 'Studio - ホーム' },
          },
          // New IA Routes
          {
            path: 'modeler',
            children: [
              {
                index: true,
                element: <ModelerHomePage />,
                handle: { title: 'Modeler - ダッシュボード' },
              },
              {
                path: 'entities',
                element: <EntityListPage />,
                handle: { title: 'Modeler - エンティティ一覧' },
              },
              {
                path: 'entities/:entityId',
                element: <EntityEditPage />,
                handle: { title: 'Modeler - エンティティ編集' },
              },
              {
                path: 'relations',
                element: <RelationViewPage />,
                handle: { title: 'Modeler - リレーション' },
              },
              {
                path: 'codes',
                element: <ModelerCodeMasterPage />,
                handle: { title: '管理 - コードマスタ' }, // Matches existing Admin title? No, this is Modeler version.
                                                        // Admin has `handle: { title: '管理 - コードマスタ' }` at line 308.
                                                        // This uses the SAME component `ModelerCodeMasterPage`.
                                                        // I should use `Modeler - コードマスタ`.
              },
              // Legacy Redirects
              {
                path: 'models',
                loader: () => redirect('../entities'),
              },
              {
                path: 'records',
                loader: () => redirect('../../data'),
              },
            ],
          },
          {
            path: 'forms',
            children: [
              {
                index: true,
                element: <FormListPage />,
                handle: { title: 'Studio - フォーム一覧' },
              },
              {
                path: ':formId',
                element: <FormDesignerPage />,
                handle: { title: 'Studio - フォームデザイナー' },
              },
            ],
          },
          {
            path: 'flows',
            children: [
              {
                index: true,
                element: <FlowListPage />,
                handle: { title: 'Studio - フロー一覧' },
              },
              {
                path: ':flowId',
                element: <FlowDesignerPage />,
                handle: { title: 'Studio - フローデザイナー' },
              },
            ],
          },
          {
            path: 'data',
            children: [
              {
                index: true,
                element: <DataBrowserPage />,
                handle: { title: 'Studio - データブラウザ' },
              },
              {
                path: ':modelId/:recordId',
                element: <DataRecordPage />,
                handle: { title: 'Studio - データレコード' },
              },
            ],
          },
          {
            path: ':modelId/releases',
            element: <ReleasePage />,
            handle: { title: 'Studio - リリースセンター' },
          },
          // Legacy Routes & Redirects
          {
            path: 'new',
            loader: () => redirect('forms/new'),
          },
          {
            path: ':modelId', // Catch-all for old form URLs
            loader: ({ params }) => redirect(`forms/${params.modelId}`),
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
