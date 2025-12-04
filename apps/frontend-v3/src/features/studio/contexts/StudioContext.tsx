import { createContext, useContext, useState, useCallback, useMemo, type ReactNode } from 'react';

/**
 * Breadcrumb item for navigation context
 */
export interface BreadcrumbItem {
  label: string;
  path?: string;
  href?: string;
}

/**
 * Workspace information
 */
export interface WorkspaceInfo {
  id: string;
  name: string;
}

/**
 * Draft status for tracking save state
 */
export type DraftStatus = 'saved' | 'unsaved' | 'saving';

/**
 * Draft information
 */
export interface DraftInfo {
  version: number;
  status: DraftStatus;
  lastSaved?: Date;
}

/**
 * Environment type
 */
export type EnvironmentType = 'dev' | 'stg' | 'prod';

/**
 * Studio context value interface
 */
export interface StudioContextValue {
  // Workspace
  workspace: WorkspaceInfo | null;
  setWorkspace: (workspace: WorkspaceInfo | null) => void;
  
  // Draft
  draft: DraftInfo;
  setDraftStatus: (status: DraftStatus) => void;
  setDraftVersion: (version: number) => void;
  markAsSaved: () => void;
  
  // Environment
  environment: EnvironmentType;
  setEnvironment: (env: EnvironmentType) => void;
  
  // Breadcrumbs
  breadcrumbs: BreadcrumbItem[];
  setBreadcrumbs: (items: BreadcrumbItem[]) => void;
  
  // UI State
  isNavigationCollapsed: boolean;
  toggleNavigation: () => void;
  isPropertiesCollapsed: boolean;
  toggleProperties: () => void;
}

const StudioContext = createContext<StudioContextValue | null>(null);

/**
 * Hook to use Studio context
 * @throws Error if used outside of StudioContextProvider
 */
export function useStudioContext(): StudioContextValue {
  const context = useContext(StudioContext);
  if (!context) {
    throw new Error('useStudioContext must be used within a StudioContextProvider');
  }
  return context;
}

/**
 * Hook to optionally use Studio context (returns null if not in provider)
 */
export function useStudioContextOptional(): StudioContextValue | null {
  return useContext(StudioContext);
}

interface StudioContextProviderProps {
  children: ReactNode;
  initialWorkspace?: WorkspaceInfo | null;
  initialEnvironment?: EnvironmentType;
}

/**
 * Studio Context Provider
 * Provides workspace, draft, environment, and UI state management for Studio
 */
export function StudioContextProvider({
  children,
  initialWorkspace = null,
  initialEnvironment = 'dev',
}: StudioContextProviderProps) {
  // Workspace state
  const [workspace, setWorkspace] = useState<WorkspaceInfo | null>(initialWorkspace);
  
  // Draft state
  const [draft, setDraft] = useState<DraftInfo>({
    version: 1,
    status: 'saved',
    lastSaved: undefined,
  });
  
  // Environment state
  const [environment, setEnvironment] = useState<EnvironmentType>(initialEnvironment);
  
  // Breadcrumbs state
  const [breadcrumbs, setBreadcrumbs] = useState<BreadcrumbItem[]>([]);
  
  // UI state
  const [isNavigationCollapsed, setIsNavigationCollapsed] = useState(false);
  const [isPropertiesCollapsed, setIsPropertiesCollapsed] = useState(false);
  
  // Draft management callbacks
  const setDraftStatus = useCallback((status: DraftStatus) => {
    setDraft((prev) => ({ ...prev, status }));
  }, []);
  
  const setDraftVersion = useCallback((version: number) => {
    setDraft((prev) => ({ ...prev, version }));
  }, []);
  
  const markAsSaved = useCallback(() => {
    setDraft((prev) => ({
      ...prev,
      status: 'saved',
      lastSaved: new Date(),
    }));
  }, []);
  
  // UI state callbacks
  const toggleNavigation = useCallback(() => {
    setIsNavigationCollapsed((prev) => !prev);
  }, []);
  
  const toggleProperties = useCallback(() => {
    setIsPropertiesCollapsed((prev) => !prev);
  }, []);
  
  const value = useMemo<StudioContextValue>(
    () => ({
      workspace,
      setWorkspace,
      draft,
      setDraftStatus,
      setDraftVersion,
      markAsSaved,
      environment,
      setEnvironment,
      breadcrumbs,
      setBreadcrumbs,
      isNavigationCollapsed,
      toggleNavigation,
      isPropertiesCollapsed,
      toggleProperties,
    }),
    [
      workspace,
      draft,
      setDraftStatus,
      setDraftVersion,
      markAsSaved,
      environment,
      breadcrumbs,
      isNavigationCollapsed,
      toggleNavigation,
      isPropertiesCollapsed,
      toggleProperties,
    ]
  );
  
  return (
    <StudioContext.Provider value={value}>
      {children}
    </StudioContext.Provider>
  );
}

export default StudioContext;
