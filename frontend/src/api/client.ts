import axios, { AxiosError } from 'axios';
import i18n from '../i18n';
import type { ApiErrorBody } from '../models/types';

const TOKEN_KEY = 'metdat.token';

export const tokenStore = {
  get: (): string | null => { try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; } },
  set: (t: string | null) => { try { t ? sessionStorage.setItem(TOKEN_KEY, t) : sessionStorage.removeItem(TOKEN_KEY); } catch { /* storage unavailable */ } },
};

export const http = axios.create({ baseURL: '/api' });

http.interceptors.request.use((cfg) => {
  const token = tokenStore.get();
  if (token) cfg.headers.set('Authorization', `Bearer ${token}`);
  cfg.headers.set('Accept-Language', i18n.language);   // the server answers (messages, errors) in the UI language
  return cfg;
});

/** Single place that turns any failure into a human readable, localised message. */
export function apiErrorMessage(err: unknown): string {
  const e = err as AxiosError<ApiErrorBody>;
  if (e?.response) {
    const body = e.response.data;
    if (body && typeof body === 'object' && 'message' in body && body.message) return body.message;
    if (e.response.status === 403) return i18n.t('errors.forbidden');
    if (e.response.status === 401) return i18n.t('errors.unauthorized');
    return i18n.t('errors.http', { status: e.response.status });
  }
  if (e?.request) return i18n.t('errors.network');
  return err instanceof Error ? err.message : i18n.t('common.unexpected');
}

export const apiFieldErrors = (err: unknown): { field: string; message: string }[] =>
  (err as AxiosError<ApiErrorBody>)?.response?.data?.fieldErrors ?? [];

export const apiErrorCode = (err: unknown): string | undefined => (err as AxiosError<ApiErrorBody>)?.response?.data?.code;

/** Fetches a protected file (bearer token required, so no plain <a href>) and hands it to the browser as a download. */
export async function downloadFile(url: string, params?: Record<string, unknown>, fallbackName = 'download'): Promise<void> {
  const res = await http.get(url, { params, responseType: 'blob' });
  const disposition: string = res.headers['content-disposition'] ?? '';
  const match = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(disposition);
  const name = match ? decodeURIComponent(match[1] ?? match[2]) : fallbackName;
  const href = URL.createObjectURL(res.data as Blob);
  const link = document.createElement('a');
  link.href = href; link.download = name;
  document.body.appendChild(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(href), 1000);
}
