import { Alert, Snackbar } from '@mui/material';
import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

type Severity = 'success' | 'error' | 'info' | 'warning';
const NotifyContext = createContext<(msg: string, severity?: Severity) => void>(() => undefined);

export function NotifyProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ msg: string; severity: Severity } | null>(null);
  const notify = useCallback((msg: string, severity: Severity = 'success') => setState({ msg, severity }), []);
  return (
    <NotifyContext.Provider value={notify}>
      {children}
      <Snackbar open={!!state} autoHideDuration={6000} onClose={() => setState(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        {state ? <Alert severity={state.severity} variant="filled" onClose={() => setState(null)}>{state.msg}</Alert> : undefined}
      </Snackbar>
    </NotifyContext.Provider>
  );
}

export const useNotify = () => useContext(NotifyContext);
