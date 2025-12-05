import React, { useEffect } from 'react';
import { useNavigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { toast } from '@mirel/ui';

export const StudioGuard: React.FC = () => {
  const { user, isAuthenticated } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) return; // Handled by root loader/guard usually

    // Check for BUILDER or ADMIN role
    // Assuming role strings based on standard convention provided in tasks
    const hasPermission = user?.roles?.some(
      (role) => role === 'BUILDER' || role === 'ADMIN' || role === 'TENANT_ADMIN'
    );

    if (!hasPermission) {
      toast({
        title: 'Access Denied',
        description: 'You do not have permission to access Studio.',
        variant: 'destructive',
      });
      navigate('/');
    }
  }, [isAuthenticated, user, navigate]);

  // Optionally show loading if auth is initializing, but verify logic usually happens after global auth loader
  if (!isAuthenticated) return null; 

  const hasPermission = user?.roles?.some(
    (role) => role === 'BUILDER' || role === 'ADMIN' || role === 'TENANT_ADMIN'
  );

  if (!hasPermission) {
    return (
      <div className="h-screen flex items-center justify-center p-8 text-center text-muted-foreground">
        Access Denied
      </div>
    );
  }

  return <Outlet />;
};
