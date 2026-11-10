import { Chip, IconButton, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow, TableSortLabel, Tooltip } from '@mui/material';
import AttachFile from '@mui/icons-material/AttachFile';
import OpenInNew from '@mui/icons-material/OpenInNew';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { Page, TestSummary } from '../models/types';
import { testDateLabel, testPeriodHint } from '../utils/format';
import { ResultChip, StatusChip } from './Chips';
import { EmptyState } from './Feedback';

const COLUMNS = [
  { key: 'testNumber', label: 'list.colId' }, { key: 'vin', label: 'list.colVin' }, { key: 'testType', label: 'list.colType' },
  { key: 'testDate', label: 'list.colDate' }, { key: 'responsiblePerson', label: 'list.colPerson' }, { key: 'status', label: 'list.colStatus' },
  { key: 'result', label: 'list.colResult' },
] as const;

interface Props {
  page: Page<TestSummary>;
  sortKey: string;
  sortDir: 'asc' | 'desc';
  hasFilters: boolean;
  onSort: (key: string, dir: 'asc' | 'desc') => void;
  onPage: (page: number) => void;
  onPageSize: (size: number) => void;
}

/** Server-paged, server-sorted table of the overview (F01). */
export function TestTable({ page, sortKey, sortDir, hasFilters, onSort, onPage, onPageSize }: Props) {
  const { t } = useTranslation();
  return (
    <>
      <TableContainer>
        <Table size="small" aria-label={t('list.tableLabel')}>
          <TableHead>
            <TableRow>
              {COLUMNS.map((c) => (
                <TableCell key={c.key} sortDirection={sortKey === c.key ? sortDir : false}>
                  <TableSortLabel active={sortKey === c.key} direction={sortKey === c.key ? sortDir : 'asc'}
                    onClick={() => onSort(c.key, sortKey === c.key && sortDir === 'asc' ? 'desc' : 'asc')}>{t(c.label)}</TableSortLabel>
                </TableCell>
              ))}
              <TableCell align="right">{t('common.open')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {page.content.map((row) => (
              <TableRow key={row.id} hover>
                <TableCell><Link to={`/tests/${row.id}`}>{row.testNumber}</Link></TableCell>
                <TableCell sx={{ fontFamily: 'monospace' }}>{row.vin}</TableCell>
                <TableCell>{row.testTypeName}{!row.testTypeActive && <Chip size="small" label={t('common.inactiveType')} sx={{ ml: 1 }} />}</TableCell>
                <TableCell><Tooltip title={testPeriodHint(row)}><span>{testDateLabel(row)}</span></Tooltip></TableCell>
                <TableCell>{row.responsiblePerson}</TableCell>
                <TableCell><StatusChip status={row.status} /></TableCell>
                <TableCell>
                  <ResultChip result={row.result} />
                  {row.attachmentCount > 0 && (
                    <Tooltip title={t('list.attachments', { count: row.attachmentCount })}>
                      <AttachFile fontSize="inherit" sx={{ ml: 1, color: 'text.secondary', verticalAlign: 'middle' }} />
                    </Tooltip>
                  )}
                </TableCell>
                <TableCell align="right">
                  <Tooltip title={t('list.openDetail')}>
                    <IconButton component={Link} to={`/tests/${row.id}`} size="small" aria-label={t('list.openTest', { id: row.testNumber })}><OpenInNew fontSize="small" /></IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {page.content.length === 0 && <EmptyState title={t('list.empty')} hint={hasFilters ? t('list.emptyFiltered') : t('list.emptyNone')} />}
      <TablePagination component="div" count={page.totalElements} page={page.number} rowsPerPage={page.size} rowsPerPageOptions={[10, 20, 50, 100]}
        labelRowsPerPage={t('list.rowsPerPage')} onPageChange={(_, p) => onPage(p)} onRowsPerPageChange={(e) => onPageSize(Number(e.target.value))} />
    </>
  );
}
