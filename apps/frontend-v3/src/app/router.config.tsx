import { createBrowserRouter } from 'react-router-dom';
import { RootLayout } from '@/layouts/RootLayout';
import { HomePage } from '@/features/home/pages/HomePage';
import ProMarkerPageWithErrorBoundary from '@/features/promarker/pages/ProMarkerPage';

/**
 * React Router v7 configuration
 * Defines application routes and navigation structure
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'promarker',
        element: <ProMarkerPageWithErrorBoundary />,
      },
      // Backward compatibility route for E2E tests
      {
        path: 'mirel/mste',
        element: <ProMarkerPageWithErrorBoundary />,
      },
    ],
  },
]);
