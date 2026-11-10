import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material';
import InboxOutlined from '@mui/icons-material/InboxOutlined';
import { apiErrorMessage } from '../api/client';

export const Loading = ({ label = 'Loading…' }: { label?: string }) => (
  <Stack alignItems="center" spacing={2} sx={{ py: 6 }} role="status" aria-live="polite">
    <CircularProgress /><Typography color="text.secondary">{label}</Typography>
  </Stack>
);

export const ErrorState = ({ error, onRetry }: { error: unknown; onRetry?: () => void }) => (
  <Alert severity="error" action={onRetry ? <Button color="inherit" size="small" onClick={onRetry}>Retry</Button> : undefined}>
    {apiErrorMessage(error)}
  </Alert>
);

export const EmptyState = ({ title, hint }: { title: string; hint?: string }) => (
  <Box sx={{ textAlign: 'center', py: 6, color: 'text.secondary' }}>
    <InboxOutlined sx={{ fontSize: 48 }} />
    <Typography variant="subtitle1">{title}</Typography>
    {hint && <Typography variant="body2">{hint}</Typography>}
  </Box>
);
