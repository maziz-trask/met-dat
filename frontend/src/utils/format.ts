import type { TestSummary, TestDetail } from '../models/types';

export const formatDateTime = (iso: string | null | undefined): string =>
  iso ? new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso)) : '—';

export const formatDate = (iso: string): string => new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' }).format(new Date(iso + 'T00:00:00'));

export const formatBytes = (n: number): string => (n < 1024 ? `${n} B` : n < 1024 * 1024 ? `${(n / 1024).toFixed(1)} KB` : `${(n / 1024 / 1024).toFixed(1)} MB`);

/** Exact date when known, otherwise the ISO calendar week as supplied (never invent a date). */
export const testDateLabel = (t: Pick<TestSummary | TestDetail, 'testDate' | 'testWeek'>): string =>
  t.testDate ? `${formatDate(t.testDate)} (${t.testWeek})` : t.testWeek;

export const testPeriodHint = (t: Pick<TestSummary | TestDetail, 'testDate' | 'periodStart' | 'periodEnd'>): string =>
  t.testDate ? 'Exact date' : `ISO week: ${formatDate(t.periodStart)} – ${formatDate(t.periodEnd)}`;

/** Mirrors the backend list of extensions for client-side pre-validation (the server remains authoritative). */
export function checkFiles(files: File[], allowed: string[], maxBytes: number): string[] {
  const problems: string[] = [];
  for (const f of files) {
    const ext = f.name.includes('.') ? f.name.split('.').pop()!.toLowerCase() : '';
    if (!allowed.includes(ext)) problems.push(`"${f.name}": unsupported type. Allowed: ${allowed.join(', ')}.`);
    else if (f.size > maxBytes) problems.push(`"${f.name}": too large (${formatBytes(f.size)}). Maximum is ${formatBytes(maxBytes)}.`);
    else if (f.size === 0) problems.push(`"${f.name}": the file is empty.`);
  }
  return problems;
}
