import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';

interface Props { open: boolean; title: string; text: string; confirmLabel?: string; busy?: boolean; onConfirm: () => void; onClose: () => void }

export function ConfirmDialog({ open, title, text, confirmLabel = 'Confirm', busy, onConfirm, onClose }: Props) {
  return (
    <Dialog open={open} onClose={onClose} aria-labelledby="confirm-title">
      <DialogTitle id="confirm-title">{title}</DialogTitle>
      <DialogContent><DialogContentText>{text}</DialogContentText></DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>Cancel</Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={busy}>{confirmLabel}</Button>
      </DialogActions>
    </Dialog>
  );
}
