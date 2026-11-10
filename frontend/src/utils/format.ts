import i18n, { currentLocale } from '../i18n';
import type { TestDetail, TestSummary } from '../models/types';

export const formatDateTime = (iso: string | null | undefined): string =>
  iso ? new Intl.DateTimeFormat(currentLocale(), { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso)) : '—';

export const formatDate = (iso: string): string => new Intl.DateTimeFormat(currentLocale(), { dateStyle: 'medium' }).format(new Date(iso + 'T00:00:00'));

export const formatBytes = (n: number): string => (n < 1024 ? `${n} B` : n < 1024 * 1024 ? `${(n / 1024).toFixed(1)} KB` : `${(n / 1024 / 1024).toFixed(1)} MB`);

type DateFields = Pick<TestSummary | TestDetail, 'testDate' | 'testWeek'>;
type PeriodFields = Pick<TestSummary | TestDetail, 'testDate' | 'periodStart' | 'periodEnd'>;

/** Exact date when known, otherwise the ISO calendar week as supplied (a date is never invented). */
export const testDateLabel = (t: DateFields): string => (t.testDate ? `${formatDate(t.testDate)} (${t.testWeek})` : t.testWeek);

export const testPeriodHint = (t: PeriodFields): string =>
  t.testDate ? i18n.t('detail.examDate') : i18n.t('detail.isoWeekPeriod', { from: formatDate(t.periodStart), to: formatDate(t.periodEnd) });

/** Mirrors the backend rules for instant feedback only; the server remains authoritative. */
export function checkFiles(files: File[], allowed: string[], maxBytes: number): string[] {
  const problems: string[] = [];
  for (const f of files) {
    const ext = f.name.includes('.') ? f.name.split('.').pop()!.toLowerCase() : '';
    if (!allowed.includes(ext)) problems.push(i18n.t('files.unsupported', { name: f.name, allowed: allowed.join(', ') }));
    else if (f.size > maxBytes) problems.push(i18n.t('files.tooLarge', { name: f.name, size: formatBytes(f.size), max: formatBytes(maxBytes) }));
    else if (f.size === 0) problems.push(i18n.t('files.empty', { name: f.name }));
  }
  return problems;
}
