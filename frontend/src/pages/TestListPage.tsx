import {
  Alert, Box, Button, Chip, IconButton, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination,
  TableRow, TableSortLabel, TextField, Tooltip, Typography,
} from '@mui/material';
import AttachFile from '@mui/icons-material/AttachFile';
import FileDownload from '@mui/icons-material/FileDownload';
import OpenInNew from '@mui/icons-material/OpenInNew';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { apiErrorMessage } from '../api/client';
import { lookupsApi, testsApi, typesApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { ResultChip, StatusChip } from '../components/Chips';
import { EmptyState, ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import type { TestFilters } from '../models/types';
import { testDateLabel, testPeriodHint } from '../utils/format';

const COLUMNS: { key: string; label: string; sortable?: boolean }[] = [
  { key: 'testNumber', label: 'Test ID', sortable: true }, { key: 'vin', label: 'Vehicle ID / VIN', sortable: true },
  { key: 'testType', label: 'Test type', sortable: true }, { key: 'testDate', label: 'Test date / week', sortable: true },
  { key: 'responsiblePerson', label: 'Responsible person', sortable: true }, { key: 'status', label: 'Status', sortable: true },
  { key: 'result', label: 'Result', sortable: true },
];

export function TestListPage() {
  const { isEditor } = useAuth();
  const notify = useNotify();
  const [params, setParams] = useSearchParams();
  const get = (k: string) => params.get(k) ?? '';
  const filters: TestFilters = { vin: get('vin'), testTypeId: get('testTypeId'), status: get('status'), dateFrom: get('dateFrom'), dateTo: get('dateTo') };
  const page = Number(get('page') || 0), size = Number(get('size') || 20), sort = get('sort') || 'testDate,desc';

  const [draft, setDraft] = useState<TestFilters>(filters);
  useEffect(() => setDraft({ vin: get('vin'), testTypeId: get('testTypeId'), status: get('status'), dateFrom: get('dateFrom'), dateTo: get('dateTo') }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [params]);
  const [exporting, setExporting] = useState(false);

  const types = useQuery({ queryKey: ['test-types', 'all'], queryFn: () => typesApi.list(false) });
  const lookups = useQuery({ queryKey: ['lookups'], queryFn: lookupsApi.get });
  const list = useQuery({
    queryKey: ['tests', filters, page, size, sort],
    queryFn: () => testsApi.list({ ...filters, page, size, sort }),
    placeholderData: keepPreviousData,
  });

  const update = (patch: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)));
    setParams(next);
  };
  const apply = (e: React.FormEvent) => {
    e.preventDefault();
    update({ vin: draft.vin?.trim(), testTypeId: draft.testTypeId, status: draft.status, dateFrom: draft.dateFrom, dateTo: draft.dateTo, page: undefined });
  };
  const reset = () => setParams(new URLSearchParams());
  const [sortKey, sortDir] = sort.split(',');
  const hasFilters = Object.values(filters).some(Boolean);
  const invalidRange = !!draft.dateFrom && !!draft.dateTo && draft.dateFrom > draft.dateTo;

  const doExport = async () => {
    setExporting(true);
    try { await testsApi.exportCsv({ ...filters, sort }); notify('Export downloaded (all records matching the current filters).'); }
    catch (e) { notify(apiErrorMessage(e), 'error'); } finally { setExporting(false); }
  };

  return (
    <>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={1} sx={{ mb: 2 }}>
        <Typography variant="h4">Technical tests</Typography>
        <Stack direction="row" spacing={1}>
          <Button startIcon={<FileDownload />} variant="outlined" onClick={doExport} disabled={exporting}>{exporting ? 'Exporting…' : 'Export CSV'}</Button>
          {isEditor && <Button variant="contained" component={Link} to="/tests/new">New test</Button>}
        </Stack>
      </Stack>

      <Paper component="form" onSubmit={apply} sx={{ p: 2, mb: 2 }} aria-label="Search and filters">
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,1fr)', lg: 'repeat(5, 1fr)' } }}>
          <TextField size="small" label="Vehicle ID / VIN" value={draft.vin ?? ''} onChange={(e) => setDraft({ ...draft, vin: e.target.value })} placeholder="contains…" />
          <TextField size="small" select label="Test type" value={draft.testTypeId ?? ''} onChange={(e) => setDraft({ ...draft, testTypeId: e.target.value })}>
            <MenuItem value="">All types</MenuItem>
            {(types.data ?? []).map((t) => <MenuItem key={t.id} value={String(t.id)}>{t.name}{t.active ? '' : ' (inactive)'}</MenuItem>)}
          </TextField>
          <TextField size="small" select label="Status" value={draft.status ?? ''} onChange={(e) => setDraft({ ...draft, status: e.target.value })}>
            <MenuItem value="">All statuses</MenuItem>
            {(lookups.data?.statuses ?? []).map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
          </TextField>
          <TextField size="small" type="date" label="Test date from" InputLabelProps={{ shrink: true }} value={draft.dateFrom ?? ''} error={invalidRange}
            onChange={(e) => setDraft({ ...draft, dateFrom: e.target.value })} />
          <TextField size="small" type="date" label="Test date to" InputLabelProps={{ shrink: true }} value={draft.dateTo ?? ''} error={invalidRange}
            helperText={invalidRange ? '"From" must not be after "To"' : undefined} onChange={(e) => setDraft({ ...draft, dateTo: e.target.value })} />
        </Box>
        <Stack direction="row" spacing={1} sx={{ mt: 2 }} alignItems="center" flexWrap="wrap" useFlexGap>
          <Button type="submit" variant="contained" disabled={invalidRange}>Search</Button>
          <Button onClick={reset} disabled={!hasFilters && !draft.vin}>Reset</Button>
          <Typography variant="caption" color="text.secondary">Date range matches tests whose ISO calendar week (Mon–Sun) overlaps the range.</Typography>
        </Stack>
      </Paper>

      {list.isLoading && <Loading label="Loading tests…" />}
      {list.error && <ErrorState error={list.error} onRetry={() => list.refetch()} />}
      {list.data && (
        <Paper>
          {list.isFetching && <Alert severity="info" icon={false} sx={{ py: 0 }}>Updating…</Alert>}
          <TableContainer>
            <Table size="small" aria-label="Technical tests">
              <TableHead>
                <TableRow>
                  {COLUMNS.map((c) => (
                    <TableCell key={c.key} sortDirection={sortKey === c.key ? (sortDir as 'asc' | 'desc') : false}>
                      <TableSortLabel active={sortKey === c.key} direction={sortKey === c.key ? (sortDir as 'asc' | 'desc') : 'asc'}
                        onClick={() => update({ sort: `${c.key},${sortKey === c.key && sortDir === 'asc' ? 'desc' : 'asc'}`, page: undefined })}>{c.label}</TableSortLabel>
                    </TableCell>
                  ))}
                  <TableCell align="right">Open</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {list.data.content.map((t) => (
                  <TableRow key={t.id} hover>
                    <TableCell><Link to={`/tests/${t.id}`}>{t.testNumber}</Link></TableCell>
                    <TableCell sx={{ fontFamily: 'monospace' }}>{t.vin}</TableCell>
                    <TableCell>{t.testTypeName}{!t.testTypeActive && <Chip size="small" label="inactive type" sx={{ ml: 1 }} />}</TableCell>
                    <TableCell><Tooltip title={testPeriodHint(t)}><span>{testDateLabel(t)}</span></Tooltip></TableCell>
                    <TableCell>{t.responsiblePerson}</TableCell>
                    <TableCell><StatusChip status={t.status} /></TableCell>
                    <TableCell><ResultChip result={t.result} />{t.attachmentCount > 0 && <Tooltip title={`${t.attachmentCount} attachment(s)`}><AttachFile fontSize="inherit" sx={{ ml: 1, color: 'text.secondary', verticalAlign: 'middle' }} /></Tooltip>}</TableCell>
                    <TableCell align="right"><Tooltip title="Open detail"><IconButton component={Link} to={`/tests/${t.id}`} size="small" aria-label={`Open ${t.testNumber}`}><OpenInNew fontSize="small" /></IconButton></Tooltip></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          {list.data.content.length === 0 && <EmptyState title="No tests found" hint={hasFilters ? 'Try changing or resetting the filters.' : 'There are no technical tests yet.'} />}
          <TablePagination component="div" count={list.data.totalElements} page={list.data.number} rowsPerPage={size} rowsPerPageOptions={[10, 20, 50, 100]}
            onPageChange={(_, p) => update({ page: p ? String(p) : undefined })}
            onRowsPerPageChange={(e) => update({ size: e.target.value, page: undefined })} />
        </Paper>
      )}
    </>
  );
}
