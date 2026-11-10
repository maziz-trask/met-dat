import { Chip, type ChipProps } from '@mui/material';
import { useTranslation } from 'react-i18next';

const STATUS_COLOR: Record<string, ChipProps['color']> = {
  Draft: 'default', Planned: 'info', 'In Progress': 'warning', Completed: 'success', Cancelled: 'error',
};
const RESULT_COLOR: Record<string, ChipProps['color']> = {
  Passed: 'success', Failed: 'error', 'Not evaluated': 'default', 'Conditionally passed': 'warning',
  'Passed with remarks': 'warning', Preliminary: 'info', Cancelled: 'default',
};

/** Status/result values are server enums; only their display text is translated. */
export function StatusChip({ status }: { status: string }) {
  const { t } = useTranslation();
  return <Chip size="small" label={t(`status.${status}`, status)} color={STATUS_COLOR[status] ?? 'default'} />;
}

export function ResultChip({ result }: { result: string | null }) {
  const { t } = useTranslation();
  if (!result) return <span aria-label={t('result.none')}>—</span>;
  return <Chip size="small" variant="outlined" label={t(`result.${result}`, result)} color={RESULT_COLOR[result] ?? 'default'} />;
}
