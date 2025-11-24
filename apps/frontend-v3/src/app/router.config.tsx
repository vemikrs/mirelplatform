import { createBrowserRouter, Outlet } from 'react-router-dom';
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
import { loadNavigationConfig } from './navigation.schema';
import ProfilePage from '@/app/settings/profile/page';
import SecurityPage from '@/app/settings/security/page';

/**
 * React Router v7 configuration
 * Defines application routes and navigation structure
 */
export const router = createBrowserRouter([
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
  {
    id: 'app-root',
    path: '/',
    element: <RootLayout />,
    loader: loadNavigationConfig,
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
            index: true,
            element: <HomePage />,
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
]);
