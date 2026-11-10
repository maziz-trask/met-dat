import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material';
import InboxOutlined from '@mui/icons-material/InboxOutlined';
import { useTranslation } from 'react-i18next';
import { apiErrorMessage } from '../api/client';

export function Loading({ label }: { label?: string }) {
  const { t } = useTranslation();
  return (
    <Stack alignItems="center" spacing={2} sx={{ py: 6 }} role="status" aria-live="polite">
      <CircularProgress /><Typography color="text.secondary">{label ?? t('common.loading')}</Typography>
    </Stack>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { t } = useTranslation();
  return (
    <Alert severity="error" action={onRetry ? <Button color="inherit" size="small" onClick={onRetry}>{t('common.retry')}</Button> : undefined}>
      {apiErrorMessage(error)}
    </Alert>
  );
}

export const EmptyState = ({ title, hint }: { title: string; hint?: string }) => (
  <Box sx={{ textAlign: 'center', py: 6, color: 'text.secondary' }}>
    <InboxOutlined sx={{ fontSize: 48 }} />
    <Typography variant="subtitle1">{title}</Typography>
    {hint && <Typography variant="body2">{hint}</Typography>}
  </Box>
);
