import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { apiErrorMessage, apiFieldErrors } from '../api/client';
import { typesApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import type { TestType } from '../models/types';

interface FormValues { name: string; description: string }

function TypeDialog({ type, onClose }: { type: TestType | 'new' | null; onClose: () => void }) {
  const qc = useQueryClient();
  const notify = useNotify();
  const [error, setError] = useState<string | null>(null);
  const editing = type && type !== 'new' ? type : null;
  const { register, handleSubmit, setError: setFieldError, formState: { errors } } = useForm<FormValues>({
    values: { name: editing?.name ?? '', description: editing?.description ?? '' },
  });
  const mut = useMutation({
    mutationFn: (v: FormValues) => (editing ? typesApi.update(editing.id, { name: v.name, description: v.description || null, active: editing.active }) : typesApi.create({ name: v.name, description: v.description || null })),
    onSuccess: (t) => { qc.invalidateQueries({ queryKey: ['test-types'] }); notify(`Test type "${t.name}" saved.`); onClose(); },
    onError: (e) => {
      const nameErr = apiFieldErrors(e).find((f) => f.field === 'name');
      if (nameErr) setFieldError('name', { message: nameErr.message }); else setError(apiErrorMessage(e));
    },
  });
  return (
    <Dialog open={!!type} onClose={onClose} fullWidth maxWidth="sm" PaperProps={{ component: "form", noValidate: true, onSubmit: handleSubmit((v) => { setError(null); mut.mutate(v); }) }}>
      <DialogTitle>{editing ? `Edit ${editing.code}` : 'Create test type'}</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Name" required autoFocus error={!!errors.name} helperText={errors.name?.message}
            {...register('name', { required: 'Name is required', maxLength: { value: 150, message: 'At most 150 characters' }, validate: (v) => !!v.trim() || 'Name is required' })} />
          <TextField label="Description" multiline minRows={2} error={!!errors.description} helperText={errors.description?.message}
            {...register('description', { maxLength: { value: 1000, message: 'At most 1000 characters' } })} />
        </Stack>
      </DialogContent>
      <DialogActions><Button onClick={onClose}>Cancel</Button><Button type="submit" variant="contained" disabled={mut.isPending}>Save</Button></DialogActions>
    </Dialog>
  );
}

export function TestTypesPage() {
  const { isEditor } = useAuth();
  const qc = useQueryClient();
  const notify = useNotify();
  const [dialog, setDialog] = useState<TestType | 'new' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { data, isLoading, error: loadError, refetch } = useQuery({ queryKey: ['test-types', 'all'], queryFn: () => typesApi.list(false) });
  const toggle = useMutation({
    mutationFn: (t: TestType) => typesApi.setActive(t.id, !t.active),
    onSuccess: (t) => { qc.invalidateQueries({ queryKey: ['test-types'] }); qc.invalidateQueries({ queryKey: ['tests'] }); notify(`"${t.name}" is now ${t.active ? 'active' : 'inactive'}.`); },
    onError: (e) => setError(apiErrorMessage(e)),
  });

  return (
    <>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h4">Test types</Typography>
        {isEditor && <Button variant="contained" onClick={() => setDialog('new')}>New test type</Button>}
      </Stack>
      <Alert severity="info" sx={{ mb: 2 }}>
        Only active types can be selected for new assignments. Deactivating a type never changes existing tests – they keep showing it.
        {!isEditor && ' You are signed in as Reader: this list is read-only.'}
      </Alert>
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {isLoading && <Loading />}
      {loadError && <ErrorState error={loadError} onRetry={() => refetch()} />}
      {data && (
        <Paper><TableContainer>
          <Table size="small" aria-label="Test types">
            <TableHead><TableRow><TableCell>ID</TableCell><TableCell>Name</TableCell><TableCell>Description</TableCell><TableCell>Status</TableCell><TableCell align="right">Used by tests</TableCell>{isEditor && <TableCell align="right">Actions</TableCell>}</TableRow></TableHead>
            <TableBody>
              {data.map((t) => (
                <TableRow key={t.id} hover>
                  <TableCell>{t.code}</TableCell><TableCell>{t.name}</TableCell>
                  <TableCell sx={{ maxWidth: 380 }}>{t.description}</TableCell>
                  <TableCell><Chip size="small" label={t.active ? 'Active' : 'Inactive'} color={t.active ? 'success' : 'default'} /></TableCell>
                  <TableCell align="right">{t.usageCount}</TableCell>
                  {isEditor && (
                    <TableCell align="right"><Box sx={{ whiteSpace: 'nowrap' }}>
                      <Button size="small" onClick={() => setDialog(t)}>Edit</Button>
                      <Button size="small" color={t.active ? 'warning' : 'success'} disabled={toggle.isPending} onClick={() => toggle.mutate(t)}>{t.active ? 'Deactivate' : 'Activate'}</Button>
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
