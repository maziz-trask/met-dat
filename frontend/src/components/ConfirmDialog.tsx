import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { useTranslation } from 'react-i18next';

interface Props { open: boolean; title: string; text: string; confirmLabel?: string; busy?: boolean; onConfirm: () => void; onClose: () => void }

export function ConfirmDialog({ open, title, text, confirmLabel, busy, onConfirm, onClose }: Props) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onClose={onClose} aria-labelledby="confirm-title">
      <DialogTitle id="confirm-title">{title}</DialogTitle>
      <DialogContent><DialogContentText>{text}</DialogContentText></DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>{t('common.cancel')}</Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={busy}>{confirmLabel ?? t('common.delete')}</Button>
      </DialogActions>
    </Dialog>
  );
}
