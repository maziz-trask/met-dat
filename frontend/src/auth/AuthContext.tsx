import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { tokenStore } from '../api/client';
import { authApi } from '../api/services';
import type { Credentials, LoginResponse, Registration, User } from '../models/types';

interface AuthState {
  user: User | null;
  loading: boolean;
  isEditor: boolean;
  demoSignIn: (userId: number) => Promise<User>;
  login: (credentials: Credentials) => Promise<User>;
  register: (registration: Registration) => Promise<User>;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/**
 * UI-side identity only. The role shown here comes from the server (/auth/me); every API call is authorised again
 * on the backend, so hiding buttons is a convenience, not the security boundary.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(!!tokenStore.get());
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!tokenStore.get()) return;
    authApi.me().then(setUser).catch(() => tokenStore.set(null)).finally(() => setLoading(false));
  }, []);

  const accept = useCallback((res: LoginResponse): User => {
    tokenStore.set(res.token);
    setUser(res.user);
    queryClient.clear();   // never show data cached under the previous identity
    return res.user;
  }, [queryClient]);

  const demoSignIn = useCallback((id: number) => authApi.demoLogin(id).then(accept), [accept]);
  const login = useCallback((c: Credentials) => authApi.login(c).then(accept), [accept]);
  const register = useCallback((r: Registration) => authApi.register(r).then(accept), [accept]);
  const signOut = useCallback(() => { tokenStore.set(null); setUser(null); queryClient.clear(); }, [queryClient]);

  const value = useMemo<AuthState>(
    () => ({ user, loading, isEditor: user?.role === 'EDITOR', demoSignIn, login, register, signOut }),
    [user, loading, demoSignIn, login, register, signOut],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
