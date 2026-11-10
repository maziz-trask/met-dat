import axios, { AxiosError } from 'axios';
import type { ApiErrorBody } from '../models/types';

const TOKEN_KEY = 'metdat.token';

export const tokenStore = {
  get: (): string | null => { try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; } },
  set: (t: string | null) => { try { t ? sessionStorage.setItem(TOKEN_KEY, t) : sessionStorage.removeItem(TOKEN_KEY); } catch { /* storage unavailable */ } },
};

export const http = axios.create({ baseURL: '/api' });

http.interceptors.request.use((cfg) => {
  const t = tokenStore.get();
  if (t) cfg.headers.set('Authorization', `Bearer ${t}`);
  return cfg;
});

/** Single place that turns any failure into a human readable message. */
export function apiErrorMessage(err: unknown): string {
  const e = err as AxiosError<ApiErrorBody>;
  if (e?.response) {
    const body = e.response.data;
    if (body && typeof body === 'object' && 'message' in body && body.message) return body.message;
    if (e.response.status === 403) return 'Your role is not allowed to perform this action.';
    if (e.response.status === 401) return 'Your session has expired. Please sign in again.';
    return `Request failed (HTTP ${e.response.status}).`;
  }
  if (e?.request) return 'The server could not be reached. Check your connection and try again.';
  return err instanceof Error ? err.message : 'Unexpected error.';
}

export function apiFieldErrors(err: unknown): { field: string; message: string }[] {
  return (err as AxiosError<ApiErrorBody>)?.response?.data?.fieldErrors ?? [];
}

export function apiErrorCode(err: unknown): string | undefined {
  return (err as AxiosError<ApiErrorBody>)?.response?.data?.code;
}

/** Fetches a protected file (bearer token required, so no plain <a href>) and hands it to the browser as a download. */
export async function downloadFile(url: string, params?: Record<string, unknown>, fallbackName = 'download'): Promise<void> {
  const res = await http.get(url, { params, responseType: 'blob' });
  const cd: string = res.headers['content-disposition'] ?? '';
  const m = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(cd);
  const name = m ? decodeURIComponent(m[1] ?? m[2]) : fallbackName;
  const href = URL.createObjectURL(res.data as Blob);
  const a = document.createElement('a');
  a.href = href; a.download = name; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(href), 1000);
}
