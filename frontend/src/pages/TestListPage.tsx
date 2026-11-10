import { Alert, Button, Menu, MenuItem, Paper, Stack, Typography } from '@mui/material';
import FileDownload from '@mui/icons-material/FileDownload';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router-dom';
import { apiErrorMessage } from '../api/client';
import { testsApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import { TestFilters } from '../components/TestFilters';
import { TestTable } from '../components/TestTable';
import { queryKeys, useLookups, useTestTypes } from '../hooks/useReferenceData';
import type { TestFilters as Filters } from '../models/types';

const FILTER_KEYS = ['vin', 'testTypeId', 'status', 'dateFrom', 'dateTo'] as const;

/** Overview (F01) with search/filter (F05) and export (F09). Filters, sort and page live in the URL: shareable and back-button friendly. */
export function TestListPage() {
  const { t } = useTranslation();
  const { isEditor } = useAuth();
  const notify = useNotify();
  const [params, setParams] = useSearchParams();

  const fromUrl = (): Filters => Object.fromEntries(FILTER_KEYS.map((k) => [k, params.get(k) ?? ''])) as Filters;
  const filters = fromUrl();
  const page = Number(params.get('page') || 0), size = Number(params.get('size') || 20), sort = params.get('sort') || 'testDate,desc';
  const [sortKey, sortDir] = sort.split(',') as [string, 'asc' | 'desc'];

  const [draft, setDraft] = useState<Filters>(filters);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => setDraft(fromUrl()), [params]);
  const [exporting, setExporting] = useState(false);
  const [exportAnchor, setExportAnchor] = useState<HTMLElement | null>(null);

  const types = useTestTypes();
  const lookups = useLookups();
  const list = useQuery({
    queryKey: [...queryKeys.tests, filters, page, size, sort],
    queryFn: () => testsApi.list({ ...filters, page, size, sort }),
    placeholderData: keepPreviousData,
  });

  const updateUrl = (patch: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)));
    setParams(next);
  };
  const hasFilters = FILTER_KEYS.some((k) => !!filters[k]);

  /** Exports ALL records matching the current filters (not just this page) in the chosen format. */
  const exportAs = async (format: 'csv' | 'xlsx') => {
    setExportAnchor(null);
    setExporting(true);
    try { await testsApi.exportFile(format, { ...filters, sort }); notify(t('list.exported')); }
    catch (e) { notify(apiErrorMessage(e), 'error'); }
    finally { setExporting(false); }
  };

  return (
    <>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={1} sx={{ mb: 2 }}>
        <Typography variant="h4">{t('list.title')}</Typography>
        <Stack direction="row" spacing={1}>
          <Button startIcon={<FileDownload />} variant="outlined" onClick={(e) => setExportAnchor(e.currentTarget)} disabled={exporting}
            aria-haspopup="menu" aria-label={t('list.exportMenu')}>{exporting ? t('list.exporting') : t('list.export')}</Button>
          <Menu anchorEl={exportAnchor} open={!!exportAnchor} onClose={() => setExportAnchor(null)}>
            <MenuItem onClick={() => exportAs('csv')}>{t('list.exportCsv')}</MenuItem>
            <MenuItem onClick={() => exportAs('xlsx')}>{t('list.exportXlsx')}</MenuItem>
          </Menu>
          {isEditor && <Button variant="contained" component={Link} to="/tests/new">{t('list.newTest')}</Button>}
        </Stack>
      </Stack>

      <TestFilters draft={draft} types={types.data ?? []} statuses={lookups.data?.statuses ?? []} canReset={hasFilters || !!draft.vin}
        onChange={setDraft} onReset={() => setParams(new URLSearchParams())}
        onApply={() => updateUrl({ vin: draft.vin?.trim(), testTypeId: draft.testTypeId, status: draft.status, dateFrom: draft.dateFrom, dateTo: draft.dateTo, page: undefined })} />

      {list.isLoading && <Loading label={t('list.loadingTests')} />}
      {list.error && <ErrorState error={list.error} onRetry={() => list.refetch()} />}
      {list.data && (
        <Paper>
          {list.isFetching && <Alert severity="info" icon={false} sx={{ py: 0 }}>{t('list.updating')}</Alert>}
          <TestTable page={list.data} sortKey={sortKey} sortDir={sortDir} hasFilters={hasFilters}
            onSort={(key, dir) => updateUrl({ sort: `${key},${dir}`, page: undefined })}
            onPage={(p) => updateUrl({ page: p ? String(p) : undefined })}
            onPageSize={(s) => updateUrl({ size: String(s), page: undefined })} />
        </Paper>
      )}
    </>
  );
}
