import { RouterProvider } from 'react-router-dom';
import { router } from './router.config.tsx';

/**
 * Router component wrapper
 * Used in App.tsx to enable routing
 */
export function AppRouter() {
  return (
    <RouterProvider 
      router={router} 
    />
  );
}
