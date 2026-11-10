import { useQuery } from '@tanstack/react-query';
import { authApi, lookupsApi, typesApi } from '../api/services';

/** Shared server-state hooks: one place for query keys and caching of reference data (nothing is hard-coded in the UI). */
export const queryKeys = {
  lookups: ['lookups'] as const,
  testTypes: ['test-types', 'all'] as const,
  authConfig: ['auth-config'] as const,
  demoUsers: ['demo-users'] as const,
  tests: ['tests'] as const,
  test: (id: string | number) => ['test', String(id)] as const,
  summary: ['summary'] as const,
  users: ['users'] as const,
};

export const useLookups = () => useQuery({ queryKey: queryKeys.lookups, queryFn: lookupsApi.get, staleTime: 5 * 60_000 });
export const useTestTypes = () => useQuery({ queryKey: queryKeys.testTypes, queryFn: () => typesApi.list(false) });
export const useAuthConfig = () => useQuery({ queryKey: queryKeys.authConfig, queryFn: authApi.config, staleTime: 5 * 60_000 });
export const useDemoUsers = (enabled = true) => useQuery({ queryKey: queryKeys.demoUsers, queryFn: authApi.demoUsers, enabled });
