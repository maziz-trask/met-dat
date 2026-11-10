import { CssBaseline, ThemeProvider } from '@mui/material';
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { createAppTheme } from './theme';

export type Mode = 'light' | 'dark';
const KEY = 'metdat.mode';

const initialMode = (): Mode => {
  try {
    const stored = localStorage.getItem(KEY);
    if (stored === 'light' || stored === 'dark') return stored;
  } catch { /* storage unavailable */ }
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

interface ThemeModeState { mode: Mode; toggle: () => void }
const ThemeModeContext = createContext<ThemeModeState>({ mode: 'light', toggle: () => undefined });

/** Light/dark mode: remembers the user's choice, otherwise follows the operating-system preference. */
export function ThemeModeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<Mode>(initialMode);
  const toggle = useCallback(() => setMode((m) => {
    const next: Mode = m === 'light' ? 'dark' : 'light';
    try { localStorage.setItem(KEY, next); } catch { /* storage unavailable */ }
    return next;
  }), []);
  const theme = useMemo(() => createAppTheme(mode), [mode]);
  const value = useMemo(() => ({ mode, toggle }), [mode, toggle]);
  return (
    <ThemeModeContext.Provider value={value}>
      <ThemeProvider theme={theme}><CssBaseline />{children}</ThemeProvider>
    </ThemeModeContext.Provider>
  );
}

export const useThemeMode = () => useContext(ThemeModeContext);
