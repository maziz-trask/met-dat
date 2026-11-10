import { Alert, Autocomplete, Box, Button, FormControlLabel, FormHelperText, MenuItem, Paper, Radio, RadioGroup, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiErrorMessage, apiFieldErrors } from '../api/client';
import { testsApi } from '../api/services';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import { queryKeys, useLookups, useTestTypes } from '../hooks/useReferenceData';
import type { TestDetail, TestRequest } from '../models/types';
import { checkFiles, formatBytes } from '../utils/format';

interface FormValues {
  vin: string; testTypeId: string; department: string; dateMode: 'date' | 'week'; testDate: string; testWeek: string;
  responsiblePerson: string; status: string; result: string; comment: string;
}
const EMPTY: FormValues = { vin: '', testTypeId: '', department: '', dateMode: 'date', testDate: '', testWeek: '', responsiblePerson: '', status: 'Draft', result: '', comment: '' };
const SERVER_FIELDS: readonly string[] = ['vin', 'testTypeId', 'department', 'testDate', 'testWeek', 'responsiblePerson', 'status', 'result', 'comment'];
const VIN_PATTERN = /^[A-HJ-NPR-Za-hj-npr-z0-9]{17}$/;

const toFormValues = (t: TestDetail): FormValues => ({
  vin: t.vin, testTypeId: String(t.testTypeId), department: t.department, dateMode: t.testDate ? 'date' : 'week', testDate: t.testDate ?? '',
  testWeek: t.testDate ? '' : t.testWeek, responsiblePerson: t.responsiblePerson, status: t.status, result: t.result ?? '', comment: t.comment ?? '',
});

const toRequest = (v: FormValues, version?: number): TestRequest => ({
  vin: v.vin.trim().toUpperCase(), testTypeId: Number(v.testTypeId), department: v.department.trim(),
  testDate: v.dateMode === 'date' ? v.testDate : null, testWeek: v.dateMode === 'week' ? v.testWeek.trim().toUpperCase() : null,
  responsiblePerson: v.responsiblePerson.trim(), status: v.status, result: v.result || null, comment: v.comment.trim() || null, version,
});

/** Create (F02) and edit (F03) share this form. Server validation is authoritative; client rules give instant feedback. */
export function TestFormPage() {
  const { t } = useTranslation();
  const { id } = useParams();
  const editing = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const notify = useNotify();
  const [files, setFiles] = useState<File[]>([]);
  const [fileProblems, setFileProblems] = useState<string[]>([]);
  const [formError, setFormError] = useState<string | null>(null);

  const existing = useQuery({ queryKey: queryKeys.test(id ?? ''), queryFn: () => testsApi.get(Number(id)), enabled: editing });
  const types = useTestTypes();
  const lookups = useLookups();

  const { control, register, handleSubmit, reset, setError, watch, formState: { errors } } = useForm<FormValues>({ defaultValues: EMPTY });
  useEffect(() => { if (existing.data) reset(toFormValues(existing.data)); }, [existing.data, reset]);

  const dateMode = watch('dateMode'), status = watch('status'), result = watch('result');
  const currentTypeId = existing.data?.testTypeId;
  // only active types may be newly selected; an already assigned (possibly inactive) type stays selectable on edit
  const selectableTypes = (types.data ?? []).filter((type) => type.active || type.id === currentTypeId);
  const completionHint = status === 'Completed' && result !== 'Passed' && result !== 'Failed';
  const allowed = lookups.data?.allowedExtensions ?? [];
  const maxBytes = lookups.data?.maxFileSizeBytes ?? 10 * 1024 * 1024;

  const save = useMutation({
    mutationFn: (v: FormValues): Promise<TestDetail> =>
      editing ? testsApi.update(Number(id), toRequest(v, existing.data?.version)) : testsApi.create(toRequest(v), files),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.tests });
      queryClient.invalidateQueries({ queryKey: queryKeys.summary });
      queryClient.invalidateQueries({ queryKey: queryKeys.test(saved.id) });
      notify(t(editing ? 'form.saved' : 'form.created', { id: saved.testNumber }));
      navigate(`/tests/${saved.id}`);
    },
    onError: (err) => {
      const issues = apiFieldErrors(err);
      let mapped = false;
      issues.forEach((issue) => {
        if (SERVER_FIELDS.includes(issue.field)) { setError(issue.field as keyof FormValues, { type: 'server', message: issue.message }); mapped = true; }
        else if (issue.field === 'files') setFileProblems([issue.message]);
      });
      setFormError(mapped ? t('form.fixFields') : apiErrorMessage(err));
    },
  });

  const onFiles = (list: FileList | null) => {
    const picked = Array.from(list ?? []);
    const problems = checkFiles(picked, allowed, maxBytes);
    setFileProblems(problems);
    setFiles(picked.filter((f) => checkFiles([f], allowed, maxBytes).length === 0));
  };

  if (editing && existing.isLoading) return <Loading />;
  if (editing && existing.error) return <ErrorState error={existing.error} onRetry={() => existing.refetch()} />;

  return (
    <Paper sx={{ p: { xs: 2, md: 3 }, maxWidth: 900 }}>
      <Typography variant="h4" gutterBottom>{editing ? t('form.editTitle', { id: existing.data?.testNumber }) : t('form.createTitle')}</Typography>
      {formError && <Alert severity="error" sx={{ mb: 2 }} role="alert">{formError}</Alert>}
      <Box component="form" noValidate onSubmit={handleSubmit((v) => { setFormError(null); save.mutate(v); })} aria-label={t('form.formLabel')}>
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
          <TextField label={t('form.vin')} required inputProps={{ maxLength: 17, style: { textTransform: 'uppercase', fontFamily: 'monospace' } }}
            error={!!errors.vin} helperText={errors.vin?.message ?? t('form.vinHelp')}
            {...register('vin', { required: t('form.vRequiredVin'), pattern: { value: VIN_PATTERN, message: t('form.vVinFormat') } })} />
          <Controller name="testTypeId" control={control} rules={{ required: t('form.vRequiredType') }} render={({ field }) => (
            <TextField select required label={t('form.type')} {...field} error={!!errors.testTypeId} helperText={errors.testTypeId?.message ?? t('form.typeHelp')}>
              {selectableTypes.map((type) => <MenuItem key={type.id} value={String(type.id)}>{type.active ? type.name : t('form.typeInactive', { name: type.name })}</MenuItem>)}
            </TextField>
          )} />
          <Controller name="department" control={control} rules={{ required: t('form.vRequiredDepartment'), maxLength: { value: 20, message: t('form.vDepartmentMax') } }} render={({ field }) => (
            <Autocomplete freeSolo options={lookups.data?.departments ?? []} inputValue={field.value} onInputChange={(_, v) => field.onChange(v)} value={field.value}
              renderInput={(p) => <TextField {...p} required label={t('form.department')} error={!!errors.department} helperText={errors.department?.message} />} />
          )} />
          <Controller name="responsiblePerson" control={control} rules={{ required: t('form.vRequiredPerson'), maxLength: { value: 150, message: t('form.vPersonMax') } }} render={({ field }) => (
            <Autocomplete freeSolo options={lookups.data?.responsiblePersons ?? []} inputValue={field.value} onInputChange={(_, v) => field.onChange(v)} value={field.value}
              renderInput={(p) => <TextField {...p} required label={t('form.person')} error={!!errors.responsiblePerson} helperText={errors.responsiblePerson?.message} />} />
          )} />
          <Box sx={{ gridColumn: { md: '1 / -1' } }}>
            <Controller name="dateMode" control={control} render={({ field }) => (
              <RadioGroup row {...field} aria-label={t('form.dateMode')}>
                <FormControlLabel value="date" control={<Radio />} label={t('form.exactDate')} />
                <FormControlLabel value="week" control={<Radio />} label={t('form.isoWeek')} />
              </RadioGroup>
            )} />
            {dateMode === 'date' ? (
              <TextField type="date" label={t('form.testDate')} required InputLabelProps={{ shrink: true }} error={!!errors.testDate} helperText={errors.testDate?.message}
                {...register('testDate', { validate: (v) => !!v || t('form.vRequiredDate') })} />
            ) : (
              <TextField label={t('form.week')} required placeholder="KT08/26" error={!!errors.testWeek} helperText={errors.testWeek?.message ?? t('form.weekHelp')}
                {...register('testWeek', { validate: (v) => /^KT\d{2}\/\d{2}$/i.test(v.trim()) || t('form.vWeekFormat') })} />
            )}
          </Box>
          <Controller name="status" control={control} rules={{ required: t('form.vRequiredStatus') }} render={({ field }) => (
            <TextField select required label={t('form.status')} {...field} error={!!errors.status} helperText={errors.status?.message}>
              {(lookups.data?.statuses ?? []).map((s) => <MenuItem key={s} value={s}>{t(`status.${s}`, s)}</MenuItem>)}
            </TextField>
          )} />
          <Controller name="result" control={control} render={({ field }) => (
            <TextField select label={t('form.result')} {...field} error={!!errors.result} helperText={errors.result?.message ?? t('common.optional')}>
              <MenuItem value=""><em>{t('common.none')}</em></MenuItem>
              {(lookups.data?.results ?? []).map((s) => <MenuItem key={s} value={s}>{t(`result.${s}`, s)}</MenuItem>)}
            </TextField>
          )} />
          <TextField label={t('form.comment')} multiline minRows={3} sx={{ gridColumn: { md: '1 / -1' } }} error={!!errors.comment} helperText={errors.comment?.message}
            {...register('comment', { maxLength: { value: 5000, message: t('form.vCommentMax') } })} />
        </Box>

        {completionHint && <Alert severity="warning" sx={{ mt: 2 }}>{t('form.completionHint')}</Alert>}

        {!editing ? (
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle1">{t('form.attachments')}</Typography>
            <Button variant="outlined" component="label" size="small">{t('form.chooseFiles')}
              <input hidden type="file" multiple data-testid="file-input" accept={allowed.map((e) => '.' + e).join(',')} onChange={(e) => onFiles(e.target.files)} />
            </Button>
            <FormHelperText>{t('form.fileLimits', { ext: allowed.join(', '), size: formatBytes(maxBytes) })}</FormHelperText>
            {files.map((f) => <Typography key={f.name} variant="body2">• {f.name} ({formatBytes(f.size)})</Typography>)}
            {fileProblems.map((p) => <Alert key={p} severity="error" sx={{ mt: 1 }}>{p}</Alert>)}
          </Box>
        ) : <Alert severity="info" sx={{ mt: 2 }}>{t('form.attachmentsInDetail')}</Alert>}

        <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
          <Button type="submit" variant="contained" disabled={save.isPending}>{save.isPending ? t('common.saving') : editing ? t('form.save') : t('form.create')}</Button>
          <Button component={Link} to={editing ? `/tests/${id}` : '/tests'}>{t('common.cancel')}</Button>
        </Stack>
      </Box>
    </Paper>
  );
}
