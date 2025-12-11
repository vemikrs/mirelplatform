import { createContext, useContext, useState, useMemo, type ReactNode } from 'react';

export interface MobileHeaderContextType {
  mobileHeaderContent: ReactNode | null;
  mobileHeaderActions: ReactNode | null;
  setMobileHeaderContent: (content: ReactNode | null) => void;
  setMobileHeaderActions: (actions: ReactNode | null) => void;
}

const MobileHeaderContext = createContext<MobileHeaderContextType | undefined>(undefined);

export function MobileHeaderProvider({ children }: { children: ReactNode }) {
  const [mobileHeaderContent, setMobileHeaderContent] = useState<ReactNode | null>(null);
  const [mobileHeaderActions, setMobileHeaderActions] = useState<ReactNode | null>(null);

  // Memoize the context value to prevent unnecessary re-renders
  const value = useMemo(
    () => ({
      mobileHeaderContent,
      mobileHeaderActions,
      setMobileHeaderContent,
      setMobileHeaderActions,
    }),
    [mobileHeaderContent, mobileHeaderActions]
  );

  return (
    <MobileHeaderContext.Provider value={value}>
      {children}
    </MobileHeaderContext.Provider>
  );
}

export function useMobileHeader() {
  const context = useContext(MobileHeaderContext);
  if (context === undefined) {
    throw new Error('useMobileHeader must be used within a MobileHeaderProvider');
  }
  return context;
}
