import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, MenuItem, Paper, Stack, Table, TableBody, TableCell,
  TableHead, TableRow, TextField, Tooltip, Typography,
} from '@mui/material';
import Delete from '@mui/icons-material/Delete';
import Download from '@mui/icons-material/Download';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiErrorMessage } from '../api/client';
import { attachmentsApi, lookupsApi, testsApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { ResultChip, StatusChip } from '../components/Chips';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import type { Attachment } from '../models/types';
import { checkFiles, formatBytes, formatDateTime, testDateLabel, testPeriodHint } from '../utils/format';

const Field = ({ label, children }: { label: string; children: React.ReactNode }) => (
  <Box><Typography variant="caption" color="text.secondary">{label}</Typography><Typography component="div">{children || '—'}</Typography></Box>
);

export function TestDetailPage() {
  const id = Number(useParams().id);
  const { isEditor } = useAuth();
  const qc = useQueryClient();
  const notify = useNotify();
  const [actionError, setActionError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<Attachment | null>(null);
  const [statusOpen, setStatusOpen] = useState(false);
  const [newStatus, setNewStatus] = useState('');

  const test = useQuery({ queryKey: ['test', String(id)], queryFn: () => testsApi.get(id) });
  const lookups = useQuery({ queryKey: ['lookups'], queryFn: lookupsApi.get });
  const refresh = () => { qc.invalidateQueries({ queryKey: ['test', String(id)] }); qc.invalidateQueries({ queryKey: ['tests'] }); qc.invalidateQueries({ queryKey: ['summary'] }); };

  const statusMut = useMutation({
    mutationFn: (s: string) => testsApi.changeStatus(id, s),
    onSuccess: (t) => { setStatusOpen(false); setActionError(null); notify(`Status changed to ${t.status}.`); refresh(); },
    onError: (e) => setActionError(apiErrorMessage(e)),   // e.g. exact reason why Completed is rejected
  });
  const uploadMut = useMutation({
    mutationFn: (files: File[]) => attachmentsApi.upload(id, files),
    onSuccess: (a) => { setActionError(null); notify(`${a.length} file(s) uploaded.`); refresh(); },
    onError: (e) => setActionError(apiErrorMessage(e)),
  });
  const deleteMut = useMutation({
    mutationFn: (a: Attachment) => attachmentsApi.remove(id, a.id),
    onSuccess: () => { setToDelete(null); setActionError(null); notify('Attachment deleted.'); refresh(); },
    onError: (e) => { setToDelete(null); setActionError(apiErrorMessage(e)); },
  });

  const onPick = (list: FileList | null, reset: () => void) => {
    const files = Array.from(list ?? []);
    const problems = checkFiles(files, lookups.data?.allowedExtensions ?? [], lookups.data?.maxFileSizeBytes ?? 10 * 1024 * 1024);
    reset();
    if (problems.length) { setActionError(problems.join(' ')); return; }
    if (files.length) uploadMut.mutate(files);
  };
  const download = async (a: Attachment) => { try { await attachmentsApi.download(id, a); } catch (e) { setActionError(apiErrorMessage(e)); } };

  if (test.isLoading) return <Loading />;
  if (test.error) return <ErrorState error={test.error} onRetry={() => test.refetch()} />;
  const t = test.data!;

  return (
    <>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={1} sx={{ mb: 2 }}>
        <Box>
          <Button component={Link} to="/tests" size="small">← Back to overview</Button>
          <Typography variant="h4" component="h1">{t.testNumber}</Typography>
          <Stack direction="row" spacing={1} sx={{ mt: 0.5 }}><StatusChip status={t.status} /><ResultChip result={t.result} /></Stack>
        </Box>
        {isEditor && (
          <Stack direction="row" spacing={1} alignItems="flex-start">
            <Button variant="outlined" onClick={() => { setNewStatus(t.status); setActionError(null); setStatusOpen(true); }}>Change status</Button>
            <Button variant="contained" component={Link} to={`/tests/${t.id}/edit`}>Edit</Button>
          </Stack>
        )}
      </Stack>

      {actionError && <Alert severity="error" sx={{ mb: 2 }} role="alert" onClose={() => setActionError(null)}>{actionError}</Alert>}

      <Paper sx={{ p: { xs: 2, md: 3 }, mb: 3 }}>
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' } }}>
          <Field label="Test ID">{t.testNumber}</Field>
          <Field label="Vehicle ID / VIN"><span style={{ fontFamily: 'monospace' }}>{t.vin}</span></Field>
          <Field label="Test type">{t.testTypeName}{!t.testTypeActive && <Chip size="small" label="inactive type" sx={{ ml: 1 }} />}</Field>
          <Field label="Department">{t.department}</Field>
          <Field label="Test date / week"><Tooltip title={testPeriodHint(t)}><span>{testDateLabel(t)}</span></Tooltip></Field>
          <Field label="Responsible person">{t.responsiblePerson}</Field>
          <Field label="Status"><StatusChip status={t.status} /></Field>
          <Field label="Result">{t.result ?? ''}</Field>
          <Box sx={{ gridColumn: '1 / -1' }}><Field label="Comment"><span style={{ whiteSpace: 'pre-wrap' }}>{t.comment}</span></Field></Box>
          <Field label="Created by">{t.createdBy}</Field>
          <Field label="Created at">{formatDateTime(t.createdAt)}</Field>
          <span />
          <Field label="Modified by">{t.modifiedBy}</Field>
          <Field label="Modified at">{formatDateTime(t.modifiedAt)}</Field>
        </Box>
      </Paper>

      <Paper sx={{ p: { xs: 2, md: 3 } }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
          <Typography variant="h6" component="h2">Attachments ({t.attachments.length})</Typography>
          {isEditor && (
            <Button variant="outlined" component="label" size="small" disabled={uploadMut.isPending}>{uploadMut.isPending ? 'Uploading…' : 'Upload files'}
              <input hidden type="file" multiple data-testid="upload-input" accept={(lookups.data?.allowedExtensions ?? []).map((e) => '.' + e).join(',')}
                onChange={(e) => onPick(e.target.files, () => { e.target.value = ''; })} />
            </Button>
          )}
        </Stack>
        {t.attachments.length === 0 ? (
          <Typography color="text.secondary">No attachments. {t.status !== 'Completed' && 'A test needs at least one attachment before it can be Completed.'}</Typography>
        ) : (
          <Table size="small" aria-label="Attachments">
            <TableHead><TableRow><TableCell>File</TableCell><TableCell>Size</TableCell><TableCell>Uploaded</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
            <TableBody>
              {t.attachments.map((a) => (
                <TableRow key={a.id}>
                  <TableCell>{a.originalFilename}{a.synthetic && <Tooltip title="Generated placeholder file for the demo – not a real document"><Chip size="small" label="synthetic" sx={{ ml: 1 }} /></Tooltip>}</TableCell>
                  <TableCell>{formatBytes(a.fileSize)}</TableCell>
                  <TableCell>{formatDateTime(a.uploadedAt)} · {a.uploadedBy}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Download"><IconButton size="small" onClick={() => download(a)} aria-label={`Download ${a.originalFilename}`}><Download fontSize="small" /></IconButton></Tooltip>
                    {isEditor && <Tooltip title="Delete"><IconButton size="small" color="error" onClick={() => setToDelete(a)} aria-label={`Delete ${a.originalFilename}`}><Delete fontSize="small" /></IconButton></Tooltip>}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        {isEditor && <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          Allowed: {(lookups.data?.allowedExtensions ?? []).join(', ')} · max {formatBytes(lookups.data?.maxFileSizeBytes ?? 0)} per file. The last attachment of a Completed test cannot be deleted.</Typography>}
      </Paper>

      <ConfirmDialog open={!!toDelete} title="Delete attachment?" text={`"${toDelete?.originalFilename}" will be permanently deleted from this test.`} confirmLabel="Delete"
        busy={deleteMut.isPending} onConfirm={() => toDelete && deleteMut.mutate(toDelete)} onClose={() => setToDelete(null)} />

      <Dialog open={statusOpen} onClose={() => setStatusOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Change status</DialogTitle>
        <DialogContent>
          {actionError && <Alert severity="error" sx={{ mb: 2 }}>{actionError}</Alert>}
          <TextField select fullWidth label="New status" value={newStatus} onChange={(e) => setNewStatus(e.target.value)} sx={{ mt: 1 }}>
            {(lookups.data?.statuses ?? []).map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
          </TextField>
          {newStatus === 'Completed' && <Typography variant="caption" color="text.secondary">Requires result Passed or Failed and at least one attachment.</Typography>}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStatusOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={statusMut.isPending || newStatus === t.status} onClick={() => statusMut.mutate(newStatus)}>Apply</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
