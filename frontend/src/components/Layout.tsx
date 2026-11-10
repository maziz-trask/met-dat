import {
  Alert, AppBar, Avatar, Box, Button, Chip, Container, Divider, IconButton, ListItemText, Menu, MenuItem, Stack, ToggleButton, ToggleButtonGroup,
  Toolbar, Tooltip, Typography,
} from '@mui/material';
import DarkMode from '@mui/icons-material/DarkMode';
import LightMode from '@mui/icons-material/LightMode';
import SwapHoriz from '@mui/icons-material/SwapHoriz';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useAuthConfig, useDemoUsers } from '../hooks/useReferenceData';
import { setLanguage, type Language } from '../i18n';
import { useThemeMode } from '../theme/ThemeModeContext';
import { useNotify } from './Notify';

const NAV = [
  { to: '/', label: 'nav.dashboard', end: true },
  { to: '/tests', label: 'nav.tests', end: false },
  { to: '/tests/new', label: 'nav.create', end: true, editorOnly: true },
  { to: '/test-types', label: 'nav.types', end: false },
  { to: '/users', label: 'nav.users', end: false, editorOnly: true },
  { to: '/about', label: 'nav.about', end: false },
];

function LanguageSwitch() {
  const { t, i18n } = useTranslation();
  return (
    <ToggleButtonGroup size="small" exclusive value={i18n.language} aria-label={t('language.label')} onChange={(_, v: Language | null) => v && setLanguage(v)}
      sx={{ '& .MuiToggleButton-root': { color: 'inherit', borderColor: 'rgba(255,255,255,0.4)', py: 0.25, px: 1 }, '& .Mui-selected': { bgcolor: 'rgba(255,255,255,0.25) !important' } }}>
      <ToggleButton value="en" aria-label="English">EN</ToggleButton>
      <ToggleButton value="cs" aria-label="Čeština">CS</ToggleButton>
    </ToggleButtonGroup>
  );
}

function ThemeToggle() {
  const { t } = useTranslation();
  const { mode, toggle } = useThemeMode();
  return (
    <Tooltip title={t('theme.toggle')}>
      <IconButton color="inherit" onClick={toggle} aria-label={t('theme.toggle')} data-testid="theme-toggle">
        {mode === 'dark' ? <LightMode /> : <DarkMode />}
      </IconButton>
    </Tooltip>
  );
}

/** Account menu: shows the verified role, lets evaluators hop between seeded demo identities, and signs out. */
function AccountMenu() {
  const { t } = useTranslation();
  const { user, demoSignIn, signOut } = useAuth();
  const notify = useNotify();
  const navigate = useNavigate();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const { data: config } = useAuthConfig();
  const { data: demoUsers } = useDemoUsers(!!anchor && !!config?.demoEnabled);
  if (!user) return null;
  const roleLabel = t(`roles.${user.role}`);

  const switchTo = async (id: number, name: string) => {
    setAnchor(null);
    try { const u = await demoSignIn(id); notify(t('auth.signedInAs', { name, role: t(`roles.${u.role}`) })); navigate('/tests'); }
    catch (e) { notify(apiErrorMessage(e), 'error'); }
  };

  return (
    <>
      <Tooltip title={t('auth.switchUser')}>
        <Button color="inherit" onClick={(e) => setAnchor(e.currentTarget)} startIcon={<SwapHoriz />} aria-label={t('auth.switchUser')} data-testid="role-switcher">
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip size="small" label={roleLabel} color={user.role === 'EDITOR' ? 'secondary' : 'default'} data-testid="role-chip" sx={{ fontWeight: 600 }} />
            <Box sx={{ display: { xs: 'none', md: 'block' } }}>{user.name}</Box>
          </Stack>
        </Button>
      </Tooltip>
      <Menu anchorEl={anchor} open={!!anchor} onClose={() => setAnchor(null)}>
        <Box sx={{ px: 2, py: 1 }}>
          <Typography fontWeight={600}>{user.name}</Typography>
          <Typography variant="caption" color="text.secondary">{user.email}</Typography>
        </Box>
        <Divider />
        {config?.demoEnabled && <Typography variant="caption" sx={{ px: 2, pt: 1, display: 'block', color: 'text.secondary' }}>{t('auth.switchDemo')}</Typography>}
        {config?.demoEnabled && (demoUsers ?? []).map((u) => (
          <MenuItem key={u.id} selected={u.id === user.id} onClick={() => switchTo(u.id, u.name)}>
            <Avatar sx={{ width: 24, height: 24, mr: 1.5, fontSize: 12, bgcolor: u.role === 'EDITOR' ? 'secondary.main' : 'grey.500' }}>{u.role[0]}</Avatar>
            <ListItemText primary={u.name} secondary={`${t(`roles.${u.role}`)} · ${u.department ?? ''}`} />
          </MenuItem>
        ))}
        {config?.demoEnabled && <Divider />}
        <MenuItem onClick={() => { setAnchor(null); signOut(); navigate('/login'); }}>{t('auth.signOut')}</MenuItem>
      </Menu>
    </>
  );
}

export function Layout() {
  const { t } = useTranslation();
  const { user, isEditor } = useAuth();
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Alert severity="warning" icon={false} sx={{ borderRadius: 0, justifyContent: 'center', py: 0 }} data-testid="demo-banner">{t('app.banner')}</Alert>
      <AppBar position="static" elevation={0}>
        <Toolbar sx={{ gap: 1, flexWrap: 'wrap' }}>
          <Typography variant="h6" component={RouterLink} to="/" sx={{ color: 'inherit', textDecoration: 'none', fontWeight: 700, mr: 2 }}>MET-DAT</Typography>
          <Box component="nav" aria-label="Main" sx={{ display: 'flex', gap: 0.5, flexGrow: 1, flexWrap: 'wrap' }}>
            {user && NAV.filter((n) => !n.editorOnly || isEditor).map((n) => (
              <Button key={n.to} component={NavLink} to={n.to} end={n.end} color="inherit" size="small"
                sx={{ opacity: 0.85, '&.active': { opacity: 1, bgcolor: 'rgba(255,255,255,0.18)' } }}>{t(n.label)}</Button>
            ))}
          </Box>
          <LanguageSwitch />
          <ThemeToggle />
          <AccountMenu />
        </Toolbar>
      </AppBar>
      <Container maxWidth="xl" component="main" sx={{ py: 3, flexGrow: 1 }}><Outlet /></Container>
      <Box component="footer" sx={{ py: 2, textAlign: 'center', color: 'text.secondary', fontSize: 13 }}>
        {t('app.footer')} · <a href="/swagger-ui.html" target="_blank" rel="noreferrer">{t('app.apiDocs')}</a>
      </Box>
    </Box>
  );
}
