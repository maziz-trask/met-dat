import { Alert, Autocomplete, Box, Button, FormControlLabel, FormHelperText, MenuItem, Paper, Radio, RadioGroup, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiErrorMessage, apiFieldErrors } from '../api/client';
import { lookupsApi, testsApi, typesApi } from '../api/services';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import type { TestDetail, TestRequest } from '../models/types';
import { checkFiles, formatBytes } from '../utils/format';

interface FormValues {
  vin: string; testTypeId: string; department: string; dateMode: 'date' | 'week'; testDate: string; testWeek: string;
  responsiblePerson: string; status: string; result: string; comment: string;
}
const EMPTY: FormValues = { vin: '', testTypeId: '', department: '', dateMode: 'date', testDate: '', testWeek: '', responsiblePerson: '', status: 'Draft', result: '', comment: '' };
const FIELDS = ['vin', 'testTypeId', 'department', 'testDate', 'testWeek', 'responsiblePerson', 'status', 'result', 'comment'] as const;

const toForm = (t: TestDetail): FormValues => ({
  vin: t.vin, testTypeId: String(t.testTypeId), department: t.department, dateMode: t.testDate ? 'date' : 'week', testDate: t.testDate ?? '',
  testWeek: t.testDate ? '' : t.testWeek, responsiblePerson: t.responsiblePerson, status: t.status, result: t.result ?? '', comment: t.comment ?? '',
});

/** Create (F02) and edit (F03) share this form. Server validation is authoritative; client rules give instant feedback. */
export function TestFormPage() {
  const { id } = useParams();
  const editing = !!id;
  const navigate = useNavigate();
  const qc = useQueryClient();
  const notify = useNotify();
  const [files, setFiles] = useState<File[]>([]);
  const [fileProblems, setFileProblems] = useState<string[]>([]);
  const [formError, setFormError] = useState<string | null>(null);

  const existing = useQuery({ queryKey: ['test', id], queryFn: () => testsApi.get(Number(id)), enabled: editing });
  const types = useQuery({ queryKey: ['test-types', 'all'], queryFn: () => typesApi.list(false) });
  const lookups = useQuery({ queryKey: ['lookups'], queryFn: lookupsApi.get });

  const { control, register, handleSubmit, reset, setError, watch, formState: { errors } } = useForm<FormValues>({ defaultValues: EMPTY });
  useEffect(() => { if (existing.data) reset(toForm(existing.data)); }, [existing.data, reset]);

  const dateMode = watch('dateMode'), status = watch('status'), result = watch('result');
  const currentTypeId = existing.data?.testTypeId;
  // only active types may be newly selected; an already assigned (possibly inactive) type stays selectable on edit
  const selectableTypes = (types.data ?? []).filter((t) => t.active || t.id === currentTypeId);
  const completionHint = status === 'Completed' && result !== 'Passed' && result !== 'Failed';

  const save = useMutation({
    mutationFn: async (v: FormValues): Promise<TestDetail> => {
      const req: TestRequest = {
        vin: v.vin.trim().toUpperCase(), testTypeId: Number(v.testTypeId), department: v.department.trim(),
        testDate: v.dateMode === 'date' ? v.testDate : null, testWeek: v.dateMode === 'week' ? v.testWeek.trim().toUpperCase() : null,
        responsiblePerson: v.responsiblePerson.trim(), status: v.status, result: v.result || null, comment: v.comment.trim() || null,
        version: existing.data?.version,
      };
      return editing ? testsApi.update(Number(id), req) : testsApi.create(req, files);
    },
    onSuccess: (t) => {
      qc.invalidateQueries({ queryKey: ['tests'] }); qc.invalidateQueries({ queryKey: ['summary'] }); qc.invalidateQueries({ queryKey: ['test', String(t.id)] });
      notify(editing ? `Test ${t.testNumber} saved.` : `Test ${t.testNumber} created.`);
      navigate(`/tests/${t.id}`);
    },
    onError: (err) => {
      const fe = apiFieldErrors(err);
      let handled = false;
      fe.forEach((f) => {
        const field = f.field === 'files' ? null : (FIELDS as readonly string[]).includes(f.field) ? (f.field as keyof FormValues) : null;
        if (field) { setError(field, { type: 'server', message: f.message }); handled = true; }
        else if (f.field === 'files') setFileProblems([f.message]);
      });
      setFormError(handled && fe.length ? 'Please correct the highlighted fields.' : apiErrorMessage(err));
    },
  });

  const onFiles = (list: FileList | null) => {
    const picked = Array.from(list ?? []);
    const problems = checkFiles(picked, lookups.data?.allowedExtensions ?? [], lookups.data?.maxFileSizeBytes ?? 10 * 1024 * 1024);
    setFileProblems(problems);
    setFiles(picked.filter((f) => !problems.some((p) => p.startsWith(`"${f.name}"`))));
  };

  if (editing && existing.isLoading) return <Loading />;
  if (editing && existing.error) return <ErrorState error={existing.error} onRetry={() => existing.refetch()} />;

  return (
    <Paper sx={{ p: { xs: 2, md: 3 }, maxWidth: 900 }}>
      <Typography variant="h4" gutterBottom>{editing ? `Edit ${existing.data?.testNumber}` : 'Create technical test'}</Typography>
      {formError && <Alert severity="error" sx={{ mb: 2 }} role="alert">{formError}</Alert>}
      <Box component="form" noValidate onSubmit={handleSubmit((v) => { setFormError(null); save.mutate(v); })} aria-label="Technical test form">
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
          <TextField label="Vehicle ID / VIN" required inputProps={{ maxLength: 17, style: { textTransform: 'uppercase', fontFamily: 'monospace' } }}
            error={!!errors.vin} helperText={errors.vin?.message ?? '17 characters, letters and digits (no I, O, Q)'}
            {...register('vin', { required: 'Vehicle ID / VIN is required', pattern: { value: /^[A-HJ-NPR-Za-hj-npr-z0-9]{17}$/, message: 'VIN must be exactly 17 letters/digits (I, O, Q are not allowed)' } })} />
          <Controller name="testTypeId" control={control} rules={{ required: 'Test type is required' }} render={({ field }) => (
            <TextField select required label="Test type" {...field} error={!!errors.testTypeId} helperText={errors.testTypeId?.message ?? 'Only active types can be newly selected'}>
              {selectableTypes.map((t) => <MenuItem key={t.id} value={String(t.id)}>{t.name}{t.active ? '' : ' (inactive – currently assigned)'}</MenuItem>)}
            </TextField>
          )} />
          <Controller name="department" control={control} rules={{ required: 'Department is required', maxLength: { value: 20, message: 'At most 20 characters' } }} render={({ field }) => (
            <Autocomplete freeSolo options={lookups.data?.departments ?? []} inputValue={field.value} onInputChange={(_, v) => field.onChange(v)} value={field.value}
              renderInput={(p) => <TextField {...p} required label="Department" error={!!errors.department} helperText={errors.department?.message} />} />
          )} />
          <Controller name="responsiblePerson" control={control} rules={{ required: 'Responsible person is required', maxLength: { value: 150, message: 'At most 150 characters' } }} render={({ field }) => (
            <Autocomplete freeSolo options={lookups.data?.responsiblePersons ?? []} inputValue={field.value} onInputChange={(_, v) => field.onChange(v)} value={field.value}
              renderInput={(p) => <TextField {...p} required label="Responsible person" error={!!errors.responsiblePerson} helperText={errors.responsiblePerson?.message} />} />
          )} />
          <Box sx={{ gridColumn: { md: '1 / -1' } }}>
            <Controller name="dateMode" control={control} render={({ field }) => (
              <RadioGroup row {...field} aria-label="Test date input mode">
                <FormControlLabel value="date" control={<Radio />} label="Exact test date" />
                <FormControlLabel value="week" control={<Radio />} label="ISO calendar week (KTww/yy)" />
              </RadioGroup>
            )} />
            {dateMode === 'date' ? (
              <TextField type="date" label="Test date" required InputLabelProps={{ shrink: true }} error={!!errors.testDate} helperText={errors.testDate?.message}
                {...register('testDate', { validate: (v) => !!v || 'Test date is required' })} />
            ) : (
              <TextField label="Calendar week" required placeholder="KT08/26" error={!!errors.testWeek} helperText={errors.testWeek?.message ?? 'Format KTww/yy, e.g. KT08/26 (Monday–Sunday of that ISO week)'}
                {...register('testWeek', { validate: (v) => /^KT\d{2}\/\d{2}$/i.test(v.trim()) || 'Calendar week must look like KT08/26' })} />
            )}
          </Box>
          <Controller name="status" control={control} rules={{ required: 'Status is required' }} render={({ field }) => (
            <TextField select required label="Status" {...field} error={!!errors.status} helperText={errors.status?.message}>
              {(lookups.data?.statuses ?? []).map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          )} />
          <Controller name="result" control={control} render={({ field }) => (
            <TextField select label="Result" {...field} error={!!errors.result} helperText={errors.result?.message ?? 'Optional'}>
              <MenuItem value=""><em>(none)</em></MenuItem>
              {(lookups.data?.results ?? []).map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          )} />
          <TextField label="Comment" multiline minRows={3} sx={{ gridColumn: { md: '1 / -1' } }} error={!!errors.comment} helperText={errors.comment?.message}
            {...register('comment', { maxLength: { value: 5000, message: 'At most 5000 characters' } })} />
        </Box>

        {completionHint && (
          <Alert severity="warning" sx={{ mt: 2 }}>
            A test can only be saved as <strong>Completed</strong> with result <em>Passed</em> or <em>Failed</em>{editing ? ' and at least one attachment' : ' and at least one attached file'}. The server will reject it otherwise.
          </Alert>
        )}

        {!editing && (
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle1">Attachments (optional)</Typography>
            <Button variant="outlined" component="label" size="small">Choose files
              <input hidden type="file" multiple data-testid="file-input" accept={(lookups.data?.allowedExtensions ?? []).map((e) => '.' + e).join(',')} onChange={(e) => onFiles(e.target.files)} />
            </Button>
            <FormHelperText>Allowed: {(lookups.data?.allowedExtensions ?? []).join(', ')} · max {formatBytes(lookups.data?.maxFileSizeBytes ?? 0)} per file</FormHelperText>
            {files.map((f) => <Typography key={f.name} variant="body2">• {f.name} ({formatBytes(f.size)})</Typography>)}
            {fileProblems.map((p) => <Alert key={p} severity="error" sx={{ mt: 1 }}>{p}</Alert>)}
          </Box>
        )}
        {editing && <Alert severity="info" sx={{ mt: 2 }}>Attachments are managed on the detail page.</Alert>}

        <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
          <Button type="submit" variant="contained" disabled={save.isPending}>{save.isPending ? 'Saving…' : editing ? 'Save changes' : 'Create test'}</Button>
          <Button component={Link} to={editing ? `/tests/${id}` : '/tests'}>Cancel</Button>
        </Stack>
      </Box>
    </Paper>
  );
}
