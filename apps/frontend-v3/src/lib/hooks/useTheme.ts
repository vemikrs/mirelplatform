import { useEffect, useState } from 'react';

const THEME_STORAGE_KEY = 'mirel-theme';

export type ThemeMode = 'light' | 'dark' | 'system';

export function useTheme() {
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    if (typeof window === 'undefined') {
      return 'system';
    }
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY) as ThemeMode | null;
    return stored || 'system';
  });

  useEffect(() => {
    if (typeof document === 'undefined') return;

    const applyTheme = () => {
      let isDark = false;
      if (themeMode === 'system') {
        isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      } else {
        isDark = themeMode === 'dark';
      }
      document.documentElement.classList.toggle('dark', isDark);
    };

    applyTheme();
    window.localStorage.setItem(THEME_STORAGE_KEY, themeMode);

    // システムモードの場合、メディアクエリの変更を監視
    if (themeMode === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handler = () => applyTheme();
      mediaQuery.addEventListener('change', handler);
      return () => mediaQuery.removeEventListener('change', handler);
    }
  }, [themeMode]);

  const toggleTheme = () => {
    setThemeMode((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const setTheme = (mode: ThemeMode) => {
    setThemeMode(mode);
  };

  const isDark = 
    themeMode === 'dark' || 
    (themeMode === 'system' && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches);

  return { theme: themeMode, themeMode, setTheme, toggleTheme, isDark };
}
