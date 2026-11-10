import { Alert, Box, Button, Chip, Paper, Stack, Tooltip, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { apiErrorMessage } from '../api/client';
import { attachmentsApi, testsApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { AttachmentPanel } from '../components/AttachmentPanel';
import { ResultChip, StatusChip } from '../components/Chips';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import { StatusChangeDialog } from '../components/StatusChangeDialog';
import { queryKeys, useLookups } from '../hooks/useReferenceData';
import type { Attachment } from '../models/types';
import { checkFiles, formatDateTime, testDateLabel, testPeriodHint } from '../utils/format';

const Field = ({ label, children }: { label: string; children?: ReactNode }) => (
  <Box><Typography variant="caption" color="text.secondary">{label}</Typography><Typography component="div">{children || '—'}</Typography></Box>
);

/** Detail (F04): all data incl. audit, attachments (F07) and the status workflow (F06). */
export function TestDetailPage() {
  const { t } = useTranslation();
  const id = Number(useParams().id);
  const { isEditor } = useAuth();
  const queryClient = useQueryClient();
  const notify = useNotify();
  const [actionError, setActionError] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<Attachment | null>(null);
  const [statusOpen, setStatusOpen] = useState(false);
  const [newStatus, setNewStatus] = useState('');

  const test = useQuery({ queryKey: queryKeys.test(id), queryFn: () => testsApi.get(id) });
  const lookups = useLookups();
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.test(id) });
    queryClient.invalidateQueries({ queryKey: queryKeys.tests });
    queryClient.invalidateQueries({ queryKey: queryKeys.summary });
  };

  const statusMutation = useMutation({
    mutationFn: (s: string) => testsApi.changeStatus(id, s),
    onSuccess: (updated) => { setStatusOpen(false); setActionError(null); notify(t('detail.statusChanged', { status: t(`status.${updated.status}`, updated.status) })); refresh(); },
    onError: (e) => setActionError(apiErrorMessage(e)),   // the exact reason why e.g. Completed is rejected
  });
  const uploadMutation = useMutation({
    mutationFn: (files: File[]) => attachmentsApi.upload(id, files),
    onSuccess: (saved) => { setActionError(null); notify(t('detail.uploaded', { count: saved.length })); refresh(); },
    onError: (e) => setActionError(apiErrorMessage(e)),
  });
  const deleteMutation = useMutation({
    mutationFn: (a: Attachment) => attachmentsApi.remove(id, a.id),
    onSuccess: () => { setToDelete(null); setActionError(null); notify(t('detail.deleted')); refresh(); },
    onError: (e) => { setToDelete(null); setActionError(apiErrorMessage(e)); },
  });

  const pickFiles = (list: FileList | null, resetInput: () => void) => {
    const files = Array.from(list ?? []);
    const problems = checkFiles(files, lookups.data?.allowedExtensions ?? [], lookups.data?.maxFileSizeBytes ?? 10 * 1024 * 1024);
    resetInput();
    if (problems.length) { setActionError(problems.join(' ')); return; }
    if (files.length) uploadMutation.mutate(files);
  };
  const download = async (a: Attachment) => { try { await attachmentsApi.download(id, a); } catch (e) { setActionError(apiErrorMessage(e)); } };

  if (test.isLoading) return <Loading />;
  if (test.error) return <ErrorState error={test.error} onRetry={() => test.refetch()} />;
  const detail = test.data!;

  return (
    <>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={1} sx={{ mb: 2 }}>
        <Box>
          <Button component={Link} to="/tests" size="small">{t('detail.back')}</Button>
          <Typography variant="h4" component="h1">{detail.testNumber}</Typography>
          <Stack direction="row" spacing={1} sx={{ mt: 0.5 }}><StatusChip status={detail.status} /><ResultChip result={detail.result} /></Stack>
        </Box>
        {isEditor && (
          <Stack direction="row" spacing={1} alignItems="flex-start">
            <Button variant="outlined" onClick={() => { setNewStatus(detail.status); setActionError(null); setStatusOpen(true); }}>{t('detail.changeStatus')}</Button>
            <Button variant="contained" component={Link} to={`/tests/${detail.id}/edit`}>{t('detail.edit')}</Button>
          </Stack>
        )}
      </Stack>

      {actionError && <Alert severity="error" sx={{ mb: 2 }} role="alert" onClose={() => setActionError(null)}>{actionError}</Alert>}

      <Paper sx={{ p: { xs: 2, md: 3 }, mb: 3 }}>
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' } }}>
          <Field label={t('detail.id')}>{detail.testNumber}</Field>
          <Field label={t('detail.vin')}><span style={{ fontFamily: 'monospace' }}>{detail.vin}</span></Field>
          <Field label={t('detail.type')}>{detail.testTypeName}{!detail.testTypeActive && <Chip size="small" label={t('common.inactiveType')} sx={{ ml: 1 }} />}</Field>
          <Field label={t('detail.department')}>{detail.department}</Field>
          <Field label={t('detail.date')}><Tooltip title={testPeriodHint(detail)}><span>{testDateLabel(detail)}</span></Tooltip></Field>
          <Field label={t('detail.person')}>{detail.responsiblePerson}</Field>
          <Field label={t('detail.status')}><StatusChip status={detail.status} /></Field>
          <Field label={t('detail.result')}>{detail.result ? t(`result.${detail.result}`, detail.result) : ''}</Field>
          <Box sx={{ gridColumn: '1 / -1' }}><Field label={t('detail.comment')}><span style={{ whiteSpace: 'pre-wrap' }}>{detail.comment}</span></Field></Box>
          <Field label={t('detail.createdBy')}>{detail.createdBy}</Field>
          <Field label={t('detail.createdAt')}>{formatDateTime(detail.createdAt)}</Field>
          <span />
          <Field label={t('detail.modifiedBy')}>{detail.modifiedBy}</Field>
          <Field label={t('detail.modifiedAt')}>{formatDateTime(detail.modifiedAt)}</Field>
        </Box>
      </Paper>

      <AttachmentPanel attachments={detail.attachments} canEdit={isEditor} showNeedHint={detail.status !== 'Completed'} uploading={uploadMutation.isPending}
        allowedExtensions={lookups.data?.allowedExtensions ?? []} maxFileSizeBytes={lookups.data?.maxFileSizeBytes ?? 0}
        onPick={pickFiles} onDownload={download} onDelete={setToDelete} />

      <ConfirmDialog open={!!toDelete} title={t('detail.deleteTitle')} text={t('detail.deleteText', { name: toDelete?.originalFilename })}
        busy={deleteMutation.isPending} onConfirm={() => toDelete && deleteMutation.mutate(toDelete)} onClose={() => setToDelete(null)} />

      <StatusChangeDialog open={statusOpen} statuses={lookups.data?.statuses ?? []} current={detail.status} value={newStatus} error={actionError}
        busy={statusMutation.isPending} onChange={setNewStatus} onApply={() => statusMutation.mutate(newStatus)} onClose={() => setStatusOpen(false)} />
    </>
  );
}
