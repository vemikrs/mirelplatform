import { createBrowserRouter } from 'react-router-dom';
import { RootLayout } from '@/layouts/RootLayout';
import { HomePage } from '@/features/home/pages/HomePage';
import { UiCatalogPage } from '@/features/catalog/pages/UiCatalogPage';
import { SiteMapPage } from '@/features/sitemap/pages/SiteMapPage';
import ProMarkerPageWithErrorBoundary from '@/features/promarker/pages/ProMarkerPage';
import { StencilEditor } from '@/features/stencil-editor/components/StencilEditor';
import { StencilListPage } from '@/features/stencil-editor/pages/StencilListPage';
import { SaaSStatusPage } from '@/features/saas-status';
import { LoginPage, SignupPage } from '@/features/auth';
import { ProtectedRoute } from '@/components/auth';
import { loadNavigationConfig } from './navigation.schema';

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
      // Backward compatibility route for E2E tests
      {
        path: 'mirel/mste',
        element: <ProMarkerPageWithErrorBoundary />,
      },
    ],
  },
]);
