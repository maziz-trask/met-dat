import { Alert, Box, Button, Card, CardActionArea, CardContent, Chip, Paper, Stack, Tab, Tabs, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, Loading } from '../components/Feedback';
import { useNotify } from '../components/Notify';
import { useAuthConfig, useDemoUsers } from '../hooks/useReferenceData';
import type { User } from '../models/types';

type Tab = 'signin' | 'register' | 'demo';
interface SignInValues { email: string; password: string }
interface RegisterValues extends SignInValues { name: string }

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function SignInForm({ onDone }: { onDone: (u: User) => void }) {
  const { t } = useTranslation();
  const { login } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<SignInValues>({ defaultValues: { email: '', password: '' } });
  return (
    <Box component="form" noValidate onSubmit={handleSubmit(async (v) => { setError(null); try { onDone(await login(v)); } catch (e) { setError(apiErrorMessage(e)); } })}>
      <Stack spacing={2}>
        {error && <Alert severity="error" role="alert">{error}</Alert>}
        <TextField label={t('auth.email')} type="email" autoComplete="username" error={!!errors.email} helperText={errors.email?.message}
          {...register('email', { required: t('auth.requiredEmail'), pattern: { value: EMAIL_PATTERN, message: t('auth.invalidEmail') } })} />
        <TextField label={t('auth.password')} type="password" autoComplete="current-password" error={!!errors.password} helperText={errors.password?.message}
          {...register('password', { required: t('auth.requiredPassword') })} />
        <Button type="submit" variant="contained" disabled={isSubmitting}>{isSubmitting ? t('auth.signingIn') : t('auth.signIn')}</Button>
      </Stack>
    </Box>
  );
}

function RegisterForm({ onDone }: { onDone: (u: User) => void }) {
  const { t } = useTranslation();
  const { register: registerUser } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterValues>({ defaultValues: { name: '', email: '', password: '' } });
  return (
    <Box component="form" noValidate onSubmit={handleSubmit(async (v) => { setError(null); try { onDone(await registerUser(v)); } catch (e) { setError(apiErrorMessage(e)); } })}>
      <Stack spacing={2}>
        <Alert severity="info">{t('auth.registerNote')}</Alert>
        {error && <Alert severity="error" role="alert">{error}</Alert>}
        <TextField label={t('auth.name')} autoComplete="name" error={!!errors.name} helperText={errors.name?.message}
          {...register('name', { required: t('auth.requiredName'), validate: (v) => !!v.trim() || t('auth.requiredName') })} />
        <TextField label={t('auth.email')} type="email" autoComplete="username" error={!!errors.email} helperText={errors.email?.message}
          {...register('email', { required: t('auth.requiredEmail'), pattern: { value: EMAIL_PATTERN, message: t('auth.invalidEmail') } })} />
        <TextField label={t('auth.password')} type="password" autoComplete="new-password" error={!!errors.password} helperText={errors.password?.message ?? t('auth.passwordHint')}
          {...register('password', { required: t('auth.requiredPassword'), minLength: { value: 8, message: t('auth.shortPassword') }, maxLength: { value: 72, message: t('auth.longPassword') } })} />
        <Button type="submit" variant="contained" disabled={isSubmitting}>{isSubmitting ? t('auth.registering') : t('auth.register')}</Button>
      </Stack>
    </Box>
  );
}

/** One-click sign-in as a seeded identity. The server verifies it and signs a token with the role stored in the database. */
function DemoUsers({ onDone }: { onDone: (u: User) => void }) {
  const { t } = useTranslation();
  const { demoSignIn } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const { data, isLoading, error: loadError, refetch } = useDemoUsers();
  const go = async (id: number) => { setError(null); try { onDone(await demoSignIn(id)); } catch (e) { setError(apiErrorMessage(e)); } };
  const editors = data?.filter((u) => u.role === 'EDITOR') ?? [];
  const readers = data?.filter((u) => u.role === 'READER') ?? [];
  if (isLoading) return <Loading />;
  if (loadError) return <ErrorState error={loadError} onRetry={() => refetch()} />;
  return (
    <>
      <Typography color="text.secondary" sx={{ mb: 2 }}>{t('auth.demoNote')}</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}>
        {editors[0] && <Button variant="contained" size="large" onClick={() => go(editors[0].id)}>{t('auth.quickEditor')}</Button>}
        {readers[0] && <Button variant="outlined" size="large" onClick={() => go(readers[0].id)}>{t('auth.quickReader')}</Button>}
      </Stack>
      {([['auth.editors', editors], ['auth.readers', readers]] as const).map(([title, list]) => (
        <Box key={title} sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>{t(title)}</Typography>
          <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' } }}>
            {list.map((u) => (
              <Card key={u.id}>
                <CardActionArea onClick={() => go(u.id)} aria-label={t('auth.signInAs', { name: u.name })}>
                  <CardContent>
                    <Typography fontWeight={600}>{u.name}</Typography>
                    <Typography variant="body2" color="text.secondary">{u.email}</Typography>
                    <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                      <Chip size="small" label={t(`roles.${u.role}`)} color={u.role === 'EDITOR' ? 'secondary' : 'default'} />
                      <Chip size="small" variant="outlined" label={u.department} />
                    </Stack>
                  </CardContent>
                </CardActionArea>
              </Card>
            ))}
          </Box>
        </Box>
      ))}
    </>
  );
}

export function LoginPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const notify = useNotify();
  const navigate = useNavigate();
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/tests';
  const { data: config, isLoading, error, refetch } = useAuthConfig();
  const [tab, setTab] = useState<Tab>('signin');
  if (user) return <Navigate to={from} replace />;

  const done = (u: User, registered = false) => {
    notify(registered ? t('auth.registered') : t('auth.signedInAs', { name: u.name, role: t(`roles.${u.role}`) }));
    navigate(from, { replace: true });
  };

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>{t('auth.title')}</Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }}>{t('auth.intro')}</Typography>
      {isLoading && <Loading />}
      {error && <ErrorState error={error} onRetry={() => refetch()} />}
      {config && (
        <Paper sx={{ p: { xs: 2, md: 3 } }}>
          <Tabs value={tab} onChange={(_, v: Tab) => setTab(v)} sx={{ mb: 3 }} variant="scrollable" allowScrollButtonsMobile>
            <Tab value="signin" label={t('auth.tabSignIn')} />
            {config.registrationEnabled && <Tab value="register" label={t('auth.tabRegister')} />}
            {config.demoEnabled && <Tab value="demo" label={t('auth.tabDemo')} />}
          </Tabs>
          <Box sx={{ maxWidth: tab === 'demo' ? undefined : 420 }}>
            {tab === 'signin' && <SignInForm onDone={(u) => done(u)} />}
            {tab === 'register' && <RegisterForm onDone={(u) => done(u, true)} />}
            {tab === 'demo' && <DemoUsers onDone={(u) => done(u)} />}
          </Box>
        </Paper>
      )}
    </Box>
  );
}
