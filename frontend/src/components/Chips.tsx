import { Chip, type ChipProps } from '@mui/material';

const STATUS_COLOR: Record<string, ChipProps['color']> = {
  Draft: 'default', Planned: 'info', 'In Progress': 'warning', Completed: 'success', Cancelled: 'error',
};
const RESULT_COLOR: Record<string, ChipProps['color']> = {
  Passed: 'success', Failed: 'error', 'Not evaluated': 'default', 'Conditionally passed': 'warning',
  'Passed with remarks': 'warning', Preliminary: 'info', Cancelled: 'default',
};

export const StatusChip = ({ status }: { status: string }) => <Chip size="small" label={status} color={STATUS_COLOR[status] ?? 'default'} />;

export const ResultChip = ({ result }: { result: string | null }) =>
  result ? <Chip size="small" variant="outlined" label={result} color={RESULT_COLOR[result] ?? 'default'} /> : <span aria-label="no result">—</span>;
