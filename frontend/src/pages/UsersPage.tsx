import { Alert, Button, Chip, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { apiErrorMessage } from '../api/client';
import { usersApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import { queryKeys } from '../hooks/useReferenceData';
import type { Role, UserSummary } from '../models/types';

/** Editor-only user administration: grant or revoke the Editor role (the backend refuses everyone else, and self-changes). */
export function UsersPage() {
  const { t } = useTranslation();
  const { user: me } = useAuth();
  const notify = useNotify();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const { data, isLoading, error: loadError, refetch } = useQuery({ queryKey: queryKeys.users, queryFn: usersApi.list });
  const change = useMutation({
    mutationFn: ({ user, role }: { user: UserSummary; role: Role }) => usersApi.changeRole(user.id, role),
    onSuccess: (u) => { setError(null); notify(t('users.roleChanged', { name: u.name, role: t(`roles.${u.role}`) })); queryClient.invalidateQueries({ queryKey: queryKeys.users }); },
    onError: (e) => setError(apiErrorMessage(e)),
  });

  return (
    <>
      <Typography variant="h4" gutterBottom>{t('users.title')}</Typography>
      <Alert severity="info" sx={{ mb: 2 }}>{t('users.info')}</Alert>
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {isLoading && <Loading />}
      {loadError && <ErrorState error={loadError} onRetry={() => refetch()} />}
      {data && (
        <Paper><TableContainer>
          <Table size="small" aria-label={t('users.title')}>
            <TableHead><TableRow>
              <TableCell>{t('users.colName')}</TableCell><TableCell>{t('users.colEmail')}</TableCell><TableCell>{t('users.colRole')}</TableCell>
              <TableCell>{t('users.colLogin')}</TableCell><TableCell align="right">{t('common.actions')}</TableCell>
            </TableRow></TableHead>
            <TableBody>
              {data.map((u) => {
                const isMe = u.id === me?.id;
                const target: Role = u.role === 'EDITOR' ? 'READER' : 'EDITOR';
                return (
                  <TableRow key={u.id} hover>
                    <TableCell>{u.name}{isMe && <Chip size="small" label={t('users.you')} sx={{ ml: 1 }} />}</TableCell>
                    <TableCell>{u.email}</TableCell>
                    <TableCell><Chip size="small" label={t(`roles.${u.role}`)} color={u.role === 'EDITOR' ? 'secondary' : 'default'} /></TableCell>
                    <TableCell>{u.passwordLogin ? t('users.passwordLogin') : t('users.demoOnly')}</TableCell>
                    <TableCell align="right">
                      <Button size="small" disabled={isMe || change.isPending} onClick={() => change.mutate({ user: u, role: target })}>
                        {target === 'EDITOR' ? t('users.makeEditor') : t('users.makeReader')}
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer></Paper>
      )}
    </>
  );
}
