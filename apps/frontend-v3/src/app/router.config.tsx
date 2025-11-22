import { createBrowserRouter } from 'react-router-dom';
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
  {
    id: 'app-root',
    path: '/',
    element: <RootLayout />,
    loader: loadNavigationConfig,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'saas-status',
        element: <SaaSStatusPage />,
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
        path: 'catalog',
        element: <UiCatalogPage />,
      },
      {
        path: 'sitemap',
        element: <SiteMapPage />,
      },
      {
        path: 'settings/profile',
        element: (
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'settings/security',
        element: (
          <ProtectedRoute>
            <SecurityPage />
          </ProtectedRoute>
        ),
      },
      // Backward compatibility route for E2E tests
      {
        path: 'mirel/mste',
        element: <ProMarkerPageWithErrorBoundary />,
      },
    ],
  },
]);
