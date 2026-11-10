import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { tokenStore } from '../api/client';
import { authApi } from '../api/services';
import type { Role, User } from '../models/types';

interface AuthState {
  user: User | null;
  loading: boolean;
  isEditor: boolean;
  signIn: (userId: number) => Promise<void>;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/**
 * UI-side identity only. The role shown here comes from the server (/auth/me); every API call is authorised again
 * on the backend from the signed token, so hiding buttons is a convenience, not the security boundary.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(!!tokenStore.get());
  const qc = useQueryClient();

  useEffect(() => {
    if (!tokenStore.get()) return;
    authApi.me().then(setUser).catch(() => tokenStore.set(null)).finally(() => setLoading(false));
  }, []);

  const signIn = useCallback(async (userId: number) => {
    const res = await authApi.login(userId);
    tokenStore.set(res.token);
    setUser(res.user);
    qc.clear();   // never show data cached under the previous identity
  }, [qc]);

  const signOut = useCallback(() => { tokenStore.set(null); setUser(null); qc.clear(); }, [qc]);

  const value = useMemo<AuthState>(() => ({ user, loading, isEditor: user?.role === ('EDITOR' as Role), signIn, signOut }), [user, loading, signIn, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
