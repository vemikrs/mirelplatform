import { createBrowserRouter, RouterProvider } from 'react-router-dom';
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
    ],
  },
]);

/**
 * Router component wrapper
 * Used in App.tsx to enable routing
 */
export function AppRouter() {
  return <RouterProvider router={router} />;
}
