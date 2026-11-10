import { Alert, Box, Button, Card, CardActionArea, CardContent, Chip, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { apiErrorMessage } from '../api/client';
import { authApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, Loading } from '../components/Feedback';

/** Demo sign-in: choose one of the fixed seeded identities. The server verifies it and signs a token with the stored role. */
export function LoginPage() {
  const { user, signIn } = useAuth();
  const navigate = useNavigate();
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/tests';
  const [error, setError] = useState<string | null>(null);
  const { data, isLoading, error: loadError, refetch } = useQuery({ queryKey: ['demo-users'], queryFn: authApi.demoUsers });
  if (user) return <Navigate to={from} replace />;

  const go = async (id: number) => { setError(null); try { await signIn(id); navigate(from, { replace: true }); } catch (e) { setError(apiErrorMessage(e)); } };
  const editors = data?.filter((u) => u.role === 'EDITOR') ?? [];
  const readers = data?.filter((u) => u.role === 'READER') ?? [];

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>Welcome to MET-DAT</Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }}>
        Technical Tests Management System – benchmark demo. This is a simplified demo sign-in: pick a seeded identity to try the application as an
        <strong> Editor</strong> (can create/edit tests, manage attachments and test types) or as a <strong>Reader</strong> (read-only). You can switch at any time from the top-right menu.
      </Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {isLoading && <Loading />}
      {loadError && <ErrorState error={loadError} onRetry={() => refetch()} />}
      {data && (
        <>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}>
            {editors[0] && <Button variant="contained" size="large" onClick={() => go(editors[0].id)}>Quick start as Editor</Button>}
            {readers[0] && <Button variant="outlined" size="large" onClick={() => go(readers[0].id)}>Quick start as Reader</Button>}
          </Stack>
          {[['Editors', editors], ['Readers', readers]].map(([title, list]) => (
            <Box key={title as string} sx={{ mb: 3 }}>
              <Typography variant="h6" gutterBottom>{title as string}</Typography>
              <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' } }}>
                {(list as typeof editors).map((u) => (
                  <Card key={u.id}><CardActionArea onClick={() => go(u.id)} aria-label={`Sign in as ${u.name}`}>
                    <CardContent>
                      <Typography fontWeight={600}>{u.name}</Typography>
                      <Typography variant="body2" color="text.secondary">{u.email}</Typography>
                      <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                        <Chip size="small" label={u.role === 'EDITOR' ? 'Editor' : 'Reader'} color={u.role === 'EDITOR' ? 'secondary' : 'default'} />
                        <Chip size="small" variant="outlined" label={u.department} />
                      </Stack>
                    </CardContent>
                  </CardActionArea></Card>
                ))}
              </Box>
            </Box>
          ))}
        </>
      )}
    </Box>
  );
}
