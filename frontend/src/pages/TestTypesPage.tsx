import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { apiErrorMessage, apiFieldErrors } from '../api/client';
import { typesApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import { queryKeys, useTestTypes } from '../hooks/useReferenceData';
import type { TestType } from '../models/types';

interface FormValues { name: string; description: string }

function TypeDialog({ type, onClose }: { type: TestType | 'new' | null; onClose: () => void }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const notify = useNotify();
  const [error, setError] = useState<string | null>(null);
  const editing = type && type !== 'new' ? type : null;
  const { register, handleSubmit, setError: setFieldError, formState: { errors } } = useForm<FormValues>({
    values: { name: editing?.name ?? '', description: editing?.description ?? '' },
  });
  const save = useMutation({
    mutationFn: (v: FormValues) => (editing
      ? typesApi.update(editing.id, { name: v.name, description: v.description || null, active: editing.active })
      : typesApi.create({ name: v.name, description: v.description || null })),
    onSuccess: (saved) => { queryClient.invalidateQueries({ queryKey: ['test-types'] }); notify(t('types.saved', { name: saved.name })); onClose(); },
    onError: (e) => {
      const nameIssue = apiFieldErrors(e).find((f) => f.field === 'name');
      if (nameIssue) setFieldError('name', { message: nameIssue.message }); else setError(apiErrorMessage(e));
    },
  });
  return (
    <Dialog open={!!type} onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: 'form', noValidate: true, onSubmit: handleSubmit((v) => { setError(null); save.mutate(v); }) }}>
      <DialogTitle>{editing ? t('types.editTitle', { code: editing.code }) : t('types.createTitle')}</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label={t('types.name')} required autoFocus error={!!errors.name} helperText={errors.name?.message}
            {...register('name', { required: t('types.vNameRequired'), maxLength: { value: 150, message: t('types.vNameMax') }, validate: (v) => !!v.trim() || t('types.vNameRequired') })} />
          <TextField label={t('types.description')} multiline minRows={2} error={!!errors.description} helperText={errors.description?.message}
            {...register('description', { maxLength: { value: 1000, message: t('types.vDescMax') } })} />
        </Stack>
      </DialogContent>
      <DialogActions><Button onClick={onClose}>{t('common.cancel')}</Button><Button type="submit" variant="contained" disabled={save.isPending}>{t('common.save')}</Button></DialogActions>
    </Dialog>
  );
}

/** Test type management (F10). Types are only ever (de)activated, never deleted, so history stays intact. */
export function TestTypesPage() {
  const { t } = useTranslation();
  const { isEditor } = useAuth();
  const queryClient = useQueryClient();
  const notify = useNotify();
  const [dialog, setDialog] = useState<TestType | 'new' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { data, isLoading, error: loadError, refetch } = useTestTypes();
  const toggle = useMutation({
    mutationFn: (type: TestType) => typesApi.setActive(type.id, !type.active),
    onSuccess: (type) => {
      queryClient.invalidateQueries({ queryKey: ['test-types'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.tests });
      notify(t(type.active ? 'types.nowActive' : 'types.nowInactive', { name: type.name }));
    },
    onError: (e) => setError(apiErrorMessage(e)),
  });

  return (
    <>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h4">{t('types.title')}</Typography>
        {isEditor && <Button variant="contained" onClick={() => setDialog('new')}>{t('types.newType')}</Button>}
      </Stack>
      <Alert severity="info" sx={{ mb: 2 }}>{t('types.info')}{!isEditor && ` ${t('types.readOnly')}`}</Alert>
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {isLoading && <Loading />}
      {loadError && <ErrorState error={loadError} onRetry={() => refetch()} />}
      {data && (
        <Paper><TableContainer>
          <Table size="small" aria-label={t('types.title')}>
            <TableHead><TableRow>
              <TableCell>{t('types.colId')}</TableCell><TableCell>{t('types.colName')}</TableCell><TableCell>{t('types.colDescription')}</TableCell>
              <TableCell>{t('types.colStatus')}</TableCell><TableCell align="right">{t('types.colUsed')}</TableCell>{isEditor && <TableCell align="right">{t('common.actions')}</TableCell>}
            </TableRow></TableHead>
            <TableBody>
              {data.map((type) => (
                <TableRow key={type.id} hover>
                  <TableCell>{type.code}</TableCell><TableCell>{type.name}</TableCell>
                  <TableCell sx={{ maxWidth: 380 }}>{type.description}</TableCell>
                  <TableCell><Chip size="small" label={type.active ? t('types.active') : t('types.inactive')} color={type.active ? 'success' : 'default'} /></TableCell>
                  <TableCell align="right">{type.usageCount}</TableCell>
                  {isEditor && (
                    <TableCell align="right"><Box sx={{ whiteSpace: 'nowrap' }}>
                      <Button size="small" onClick={() => setDialog(type)}>{t('common.edit')}</Button>
                      <Button size="small" color={type.active ? 'warning' : 'success'} disabled={toggle.isPending} onClick={() => toggle.mutate(type)}>
                        {type.active ? t('types.deactivate') : t('types.activate')}
                      </Button>
                    </Box></TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer></Paper>
      )}
      <TypeDialog type={dialog} onClose={() => setDialog(null)} />
    </>
  );
}
