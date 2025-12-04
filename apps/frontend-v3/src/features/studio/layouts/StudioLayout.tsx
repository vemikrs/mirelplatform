import React, { type ReactNode } from 'react';
import { cn } from '@mirel/ui';
import { StudioContextProvider, useStudioContext, type WorkspaceInfo, type EnvironmentType } from '../contexts';
import { StudioNavigation, type StudioNavItem } from '../components/StudioNavigation';
import { StudioHeader } from '../components/StudioHeader';

interface StudioLayoutProps {
  children: ReactNode;
  /**
   * Custom navigation component or items
   */
  navigation?: ReactNode | StudioNavItem[];
  /**
   * Explorer panel (left side, after navigation)
   */
  explorer?: ReactNode;
  /**
   * Properties panel (right side)
   */
  properties?: ReactNode;
  /**
   * Whether to hide the navigation
   */
  hideNavigation?: boolean;
  /**
   * Whether to hide the properties panel
   */
  hideProperties?: boolean;
  /**
   * Whether to show the header
   */
  showHeader?: boolean;
  /**
   * Additional class names
   */
  className?: string;
  /**
   * Initial workspace info
   */
  initialWorkspace?: WorkspaceInfo | null;
  /**
   * Initial environment
   */
  initialEnvironment?: EnvironmentType;
}

/**
 * Studio Layout Component
 * Provides the unified 3-pane layout for all Studio screens
 * 
 * Structure:
 * ┌─────────────────────────────────────────────────────────┐
 * │ Header (optional)                                       │
 * ├────────┬─────────────────────────────────────┬──────────┤
 * │ Nav    │ Explorer │ Main Content Area        │ Property │
 * │ Panel  │ (opt)    │                          │ Panel    │
 * │        │          │                          │ (opt)    │
 * └────────┴──────────┴──────────────────────────┴──────────┘
 */
export function StudioLayout({
  children,
  navigation,
  explorer,
  properties,
  hideNavigation = false,
  hideProperties = false,
  showHeader = true,
  className,
  initialWorkspace,
  initialEnvironment = 'dev',
}: StudioLayoutProps) {
  return (
    <StudioContextProvider
      initialWorkspace={initialWorkspace}
      initialEnvironment={initialEnvironment}
    >
      <StudioLayoutInner
        navigation={navigation}
        explorer={explorer}
        properties={properties}
        hideNavigation={hideNavigation}
        hideProperties={hideProperties}
        showHeader={showHeader}
        className={className}
      >
        {children}
      </StudioLayoutInner>
    </StudioContextProvider>
  );
}

interface StudioLayoutInnerProps {
  children: ReactNode;
  navigation?: ReactNode | StudioNavItem[];
  explorer?: ReactNode;
  properties?: ReactNode;
  hideNavigation: boolean;
  hideProperties: boolean;
  showHeader: boolean;
  className?: string;
}

function StudioLayoutInner({
  children,
  navigation,
  explorer,
  properties,
  hideNavigation,
  hideProperties,
  showHeader,
  className,
}: StudioLayoutInnerProps) {
  const {
    isNavigationCollapsed,
    toggleNavigation,
    isPropertiesCollapsed,
  } = useStudioContext();

  // Render navigation
  const renderNavigation = () => {
    if (hideNavigation) return null;
    
    if (React.isValidElement(navigation)) {
      return navigation;
    }
    
    const navItems = Array.isArray(navigation) ? navigation : undefined;
    
    return (
      <StudioNavigation
        items={navItems}
        collapsed={isNavigationCollapsed}
        className="hidden md:flex shrink-0"
      />
    );
  };

  return (
    <div className={cn('flex flex-col h-screen overflow-hidden bg-background', className)}>
      {/* Header */}
      {showHeader && (
        <StudioHeader
          onToggleNavigation={toggleNavigation}
          isNavigationCollapsed={isNavigationCollapsed}
        />
      )}

      {/* Main Content Area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Navigation */}
        {renderNavigation()}

        {/* Explorer Panel */}
        {explorer && (
          <div className="w-64 shrink-0 border-r border-outline/20 bg-surface overflow-hidden hidden lg:block">
            {explorer}
          </div>
        )}

        {/* Main Canvas / Content */}
        <main className="flex-1 overflow-hidden bg-background">
          {children}
        </main>

        {/* Properties Panel */}
        {!hideProperties && properties && (
          <div
            className={cn(
              'shrink-0 border-l border-outline/20 bg-surface overflow-hidden hidden lg:block transition-all duration-200',
              isPropertiesCollapsed ? 'w-10' : 'w-80'
            )}
          >
            {properties}
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * StudioLayout with custom wrapper
 * Use when you need direct access to context without provider re-creation
 */
export function StudioLayoutContent({
  children,
  explorer,
  properties,
  hideNavigation = false,
  hideProperties = false,
  showHeader = true,
  className,
}: Omit<StudioLayoutProps, 'initialWorkspace' | 'initialEnvironment'>) {
  const {
    isNavigationCollapsed,
    toggleNavigation,
    isPropertiesCollapsed,
  } = useStudioContext();

  return (
    <div className={cn('flex flex-col h-screen overflow-hidden bg-background', className)}>
      {/* Header */}
      {showHeader && (
        <StudioHeader
          onToggleNavigation={toggleNavigation}
          isNavigationCollapsed={isNavigationCollapsed}
        />
      )}

      {/* Main Content Area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Navigation */}
        {!hideNavigation && (
          <StudioNavigation
            collapsed={isNavigationCollapsed}
            className="hidden md:flex shrink-0"
          />
        )}

        {/* Explorer Panel */}
        {explorer && (
          <div className="w-64 shrink-0 border-r border-outline/20 bg-surface overflow-hidden hidden lg:block">
            {explorer}
          </div>
        )}

        {/* Main Canvas / Content */}
        <main className="flex-1 overflow-hidden bg-background">
          {children}
        </main>

        {/* Properties Panel */}
        {!hideProperties && properties && (
          <div
            className={cn(
              'shrink-0 border-l border-outline/20 bg-surface overflow-hidden hidden lg:block transition-all duration-200',
              isPropertiesCollapsed ? 'w-10' : 'w-80'
            )}
          >
            {properties}
          </div>
        )}
      </div>
    </div>
  );
}

export default StudioLayout;
