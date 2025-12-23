import { useTheme as useUiTheme } from '@mirel/ui';

export type ThemeMode = 'light' | 'dark' | 'system';

export function useTheme() {
  const { theme, setTheme } = useUiTheme();

  const toggleTheme = () => {
    setTheme(theme === 'dark' ? 'light' : 'dark');
  };

  const isDark = 
    theme === 'dark' || 
    (theme === 'system' && typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches);

  return { theme: theme as ThemeMode, themeMode: theme as ThemeMode, setTheme, toggleTheme, isDark };
}
