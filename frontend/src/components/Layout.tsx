import {
  AppBar, Alert, Avatar, Box, Button, Chip, Container, Divider, IconButton, ListItemText, Menu, MenuItem, Stack, Toolbar, Tooltip, Typography,
} from '@mui/material';
import SwapHoriz from '@mui/icons-material/SwapHoriz';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link as RouterLink, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { authApi } from '../api/services';
import { useAuth } from '../auth/AuthContext';
import { useNotify } from './Notify';

const NAV = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/tests', label: 'Technical Tests', end: false },
  { to: '/tests/new', label: 'Create Test', end: true, editorOnly: true },
  { to: '/test-types', label: 'Test Types', end: false },
  { to: '/about', label: 'About / API', end: false },
];

function RoleSwitcher() {
  const { user, signIn, signOut } = useAuth();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const notify = useNotify();
  const navigate = useNavigate();
  const { data: users } = useQuery({ queryKey: ['demo-users'], queryFn: authApi.demoUsers, enabled: !!anchor });
  if (!user) return null;
  const roleLabel = user.role === 'EDITOR' ? 'Editor' : 'Reader';
  return (
    <>
      <Tooltip title="Switch demo user / role">
        <Button color="inherit" onClick={(e) => setAnchor(e.currentTarget)} startIcon={<SwapHoriz />} aria-label="Switch user" data-testid="role-switcher">
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip size="small" label={roleLabel} color={user.role === 'EDITOR' ? 'secondary' : 'default'} data-testid="role-chip" sx={{ fontWeight: 600 }} />
            <Box sx={{ display: { xs: 'none', md: 'block' } }}>{user.name}</Box>
          </Stack>
        </Button>
      </Tooltip>
      <Menu anchorEl={anchor} open={!!anchor} onClose={() => setAnchor(null)}>
        <Typography variant="caption" sx={{ px: 2, color: 'text.secondary' }}>Switch demo identity</Typography>
        {(users ?? []).map((u) => (
          <MenuItem key={u.id} selected={u.id === user.id} onClick={async () => {
            setAnchor(null);
            try { await signIn(u.id); notify(`Signed in as ${u.name} (${u.role === 'EDITOR' ? 'Editor' : 'Reader'})`); navigate('/tests'); } catch { notify('Sign-in failed', 'error'); }
          }}>
            <Avatar sx={{ width: 24, height: 24, mr: 1.5, fontSize: 12, bgcolor: u.role === 'EDITOR' ? 'secondary.main' : 'grey.500' }}>{u.role[0]}</Avatar>
            <ListItemText primary={u.name} secondary={`${u.role === 'EDITOR' ? 'Editor' : 'Reader'} · ${u.department ?? ''}`} />
          </MenuItem>
        ))}
        <Divider />
        <MenuItem onClick={() => { setAnchor(null); signOut(); navigate('/login'); }}>Sign out</MenuItem>
      </Menu>
    </>
  );
}

export function Layout() {
  const { user, isEditor } = useAuth();
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Alert severity="warning" icon={false} sx={{ borderRadius: 0, justifyContent: 'center', py: 0 }} data-testid="demo-banner">
        <strong>DEMONSTRATION SYSTEM</strong> – all vehicles, people and e-mail addresses are synthetic benchmark data. Attachments are generated placeholder files.
      </Alert>
      <AppBar position="static" elevation={0}>
        <Toolbar sx={{ gap: 1, flexWrap: 'wrap' }}>
          <Typography variant="h6" component={RouterLink} to="/" sx={{ color: 'inherit', textDecoration: 'none', fontWeight: 700, mr: 2 }}>MET-DAT</Typography>
          <Box component="nav" aria-label="Main" sx={{ display: 'flex', gap: 0.5, flexGrow: 1, flexWrap: 'wrap' }}>
            {user && NAV.filter((n) => !n.editorOnly || isEditor).map((n) => (
              <Button key={n.to} component={NavLink} to={n.to} end={n.end} color="inherit" size="small"
                sx={{ opacity: 0.85, '&.active': { opacity: 1, bgcolor: 'rgba(255,255,255,0.18)' } }}>{n.label}</Button>
            ))}
          </Box>
          <RoleSwitcher />
        </Toolbar>
      </AppBar>
      <Container maxWidth="xl" component="main" sx={{ py: 3, flexGrow: 1 }}><Outlet /></Container>
      <Box component="footer" sx={{ py: 2, textAlign: 'center', color: 'text.secondary', fontSize: 13 }}>
        MET-DAT benchmark demo · synthetic data · <a href="/swagger-ui.html" target="_blank" rel="noreferrer">API documentation</a>
        <IconButton size="small" sx={{ display: 'none' }} />
      </Box>
    </Box>
  );
}
