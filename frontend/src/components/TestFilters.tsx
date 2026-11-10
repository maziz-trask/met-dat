import { Box, Button, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { TestFilters as Filters, TestType } from '../models/types';

interface Props {
  draft: Filters;
  types: TestType[];
  statuses: string[];
  canReset: boolean;
  onChange: (next: Filters) => void;
  onApply: () => void;
  onReset: () => void;
}

/** Search form of the overview (F05). Values are sent to the server only when "Search" is pressed. */
export function TestFilters({ draft, types, statuses, canReset, onChange, onApply, onReset }: Props) {
  const { t } = useTranslation();
  const invalidRange = !!draft.dateFrom && !!draft.dateTo && draft.dateFrom > draft.dateTo;
  const set = (patch: Partial<Filters>) => onChange({ ...draft, ...patch });
  return (
    <Paper component="form" onSubmit={(e: React.FormEvent) => { e.preventDefault(); onApply(); }} sx={{ p: 2, mb: 2 }} aria-label={t('list.filters')}>
      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,1fr)', lg: 'repeat(5, 1fr)' } }}>
        <TextField size="small" label={t('list.vin')} value={draft.vin ?? ''} onChange={(e) => set({ vin: e.target.value })} placeholder={t('list.vinPlaceholder')} />
        <TextField size="small" select label={t('list.type')} value={draft.testTypeId ?? ''} onChange={(e) => set({ testTypeId: e.target.value })}>
          <MenuItem value="">{t('list.allTypes')}</MenuItem>
          {types.map((type) => <MenuItem key={type.id} value={String(type.id)}>{type.name}{type.active ? '' : ` (${t('common.inactiveType')})`}</MenuItem>)}
        </TextField>
        <TextField size="small" select label={t('list.status')} value={draft.status ?? ''} onChange={(e) => set({ status: e.target.value })}>
          <MenuItem value="">{t('list.allStatuses')}</MenuItem>
          {statuses.map((s) => <MenuItem key={s} value={s}>{t(`status.${s}`, s)}</MenuItem>)}
        </TextField>
        <TextField size="small" type="date" label={t('list.dateFrom')} InputLabelProps={{ shrink: true }} value={draft.dateFrom ?? ''} error={invalidRange}
          onChange={(e) => set({ dateFrom: e.target.value })} />
        <TextField size="small" type="date" label={t('list.dateTo')} InputLabelProps={{ shrink: true }} value={draft.dateTo ?? ''} error={invalidRange}
          helperText={invalidRange ? t('list.rangeError') : undefined} onChange={(e) => set({ dateTo: e.target.value })} />
      </Box>
      <Stack direction="row" spacing={1} sx={{ mt: 2 }} alignItems="center" flexWrap="wrap" useFlexGap>
        <Button type="submit" variant="contained" disabled={invalidRange}>{t('common.search')}</Button>
        <Button onClick={onReset} disabled={!canReset}>{t('common.reset')}</Button>
        <Typography variant="caption" color="text.secondary">{t('list.weekHint')}</Typography>
      </Stack>
    </Paper>
  );
}
