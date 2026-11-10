import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

interface Props {
  open: boolean;
  statuses: string[];
  current: string;
  value: string;
  error: string | null;
  busy: boolean;
  onChange: (status: string) => void;
  onApply: () => void;
  onClose: () => void;
}

/** Status transition dialog (F06): the server's rejection reason is shown verbatim inside the dialog. */
export function StatusChangeDialog({ open, statuses, current, value, error, busy, onChange, onApply, onClose }: Props) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{t('detail.statusTitle')}</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <TextField select fullWidth label={t('detail.newStatus')} value={value} onChange={(e) => onChange(e.target.value)} sx={{ mt: 1 }}>
          {statuses.map((s) => <MenuItem key={s} value={s}>{t(`status.${s}`, s)}</MenuItem>)}
        </TextField>
        {value === 'Completed' && <Typography variant="caption" color="text.secondary">{t('detail.completedNeeds')}</Typography>}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.cancel')}</Button>
        <Button variant="contained" disabled={busy || value === current} onClick={onApply}>{t('detail.apply')}</Button>
      </DialogActions>
    </Dialog>
  );
}
