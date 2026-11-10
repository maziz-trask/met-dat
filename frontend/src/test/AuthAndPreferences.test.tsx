import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n, { setLanguage } from '../i18n';
import { StatusChip } from '../components/Chips';
import { LoginPage } from '../pages/LoginPage';
import { UsersPage } from '../pages/UsersPage';
import { ThemeModeProvider, useThemeMode } from '../theme/ThemeModeContext';
import { renderRoute } from './helpers';

const auth = vi.hoisted(() => ({ login: vi.fn(), register: vi.fn(), demoSignIn: vi.fn(), user: null as null | { id: number; name: string } }));
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: auth.user, loading: false, isEditor: true, login: auth.login, register: auth.register, demoSignIn: auth.demoSignIn }),
}));
vi.mock('../api/services', () => ({
  authApi: {
    config: vi.fn().mockResolvedValue({ demoEnabled: true, registrationEnabled: true }),
    demoUsers: vi.fn().mockResolvedValue([
      { id: 1, code: 'U001', name: 'Adam Novotný', email: 'adam@example.com', role: 'EDITOR', department: 'ESW' },
      { id: 7, code: 'U007', name: 'Jakub Dvořák', email: 'jakub@example.com', role: 'READER', department: 'ESW' },
    ]),
  },
  usersApi: {
    list: vi.fn().mockResolvedValue([
      { id: 1, code: 'U001', name: 'Adam Novotný', email: 'adam@example.com', role: 'EDITOR', department: 'ESW', active: true, passwordLogin: false },
      { id: 11, code: 'U011', name: 'Nina New', email: 'nina@example.com', role: 'READER', department: null, active: true, passwordLogin: true },
    ]),
    changeRole: vi.fn(),
  },
}));
import { usersApi } from '../api/services';

beforeEach(() => {
  auth.login.mockReset(); auth.register.mockReset(); auth.demoSignIn.mockReset(); auth.user = null;
  setLanguage('en');
});

describe('Sign-in and registration', () => {
  it('validates e-mail and password before calling the API', async () => {
    renderRoute(<LoginPage />, '/login', '/login');
    await userEvent.click(await screen.findByRole('button', { name: 'Sign in' }));
    expect(await screen.findByText('E-mail is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('shows the server message when credentials are wrong', async () => {
    auth.login.mockRejectedValue({ response: { status: 401, data: { code: 'INVALID_CREDENTIALS', message: 'Invalid e-mail or password.' } } });
    renderRoute(<LoginPage />, '/login', '/login');
    await userEvent.type(await screen.findByLabelText('E-mail'), 'a@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'whatever1');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid e-mail or password.');
  });

  it('registration enforces an 8 character password and sends no role', async () => {
    auth.register.mockResolvedValue({ id: 5, name: 'N', role: 'READER' });
    renderRoute(<LoginPage />, '/login', '/login');
    await userEvent.click(await screen.findByRole('tab', { name: 'Create account' }));
    await userEvent.type(screen.getByLabelText('Full name'), 'Nina New');
    await userEvent.type(screen.getByLabelText('E-mail'), 'nina@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'short');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByText('Password must have at least 8 characters')).toBeInTheDocument();
    await userEvent.clear(screen.getByLabelText('Password'));
    await userEvent.type(screen.getByLabelText('Password'), 'longenough1');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(auth.register).toHaveBeenCalledWith({ name: 'Nina New', email: 'nina@example.com', password: 'longenough1' }));
  });

  it('demo tab lists seeded identities and signs in with one click', async () => {
    auth.demoSignIn.mockResolvedValue({ id: 7, name: 'Jakub Dvořák', role: 'READER' });
    renderRoute(<LoginPage />, '/login', '/login');
    await userEvent.click(await screen.findByRole('tab', { name: 'Demo users' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Quick start as Reader' }));
    await waitFor(() => expect(auth.demoSignIn).toHaveBeenCalledWith(7));
  });
});

describe('Language and theme', () => {
  it('translates status chips and UI text to Czech and back', async () => {
    const { rerender } = render(<StatusChip status="In Progress" />);
    expect(screen.getByText('In Progress')).toBeInTheDocument();
    await act(async () => { setLanguage('cs'); });
    rerender(<StatusChip status="In Progress" />);
    expect(screen.getByText('Probíhá')).toBeInTheDocument();
    expect(i18n.t('app.footer')).toContain('syntetická data');
    await act(async () => { setLanguage('en'); });
  });

  it('every English key has a Czech translation (same shape)', async () => {
    const en = (await import('../i18n/locales/en')).default, cs = (await import('../i18n/locales/cs')).default;
    const keys = (o: object, p = ''): string[] => Object.entries(o).flatMap(([k, v]) => (typeof v === 'object' ? keys(v as object, `${p}${k}.`) : [`${p}${k}`]));
    expect(keys(cs)).toEqual(keys(en));
  });

  it('theme toggle switches between light and dark and remembers the choice', async () => {
    localStorage.removeItem('metdat.mode');
    function Probe() { const { mode, toggle } = useThemeMode(); return <button onClick={toggle}>mode:{mode}</button>; }
    render(<ThemeModeProvider><Probe /></ThemeModeProvider>);
    const button = screen.getByRole('button');
    expect(button).toHaveTextContent('mode:light');
    await userEvent.click(button);
    expect(button).toHaveTextContent('mode:dark');
    expect(localStorage.getItem('metdat.mode')).toBe('dark');
  });
});

describe('User administration', () => {
  it('lets an Editor change other users but not themselves', async () => {
    auth.user = { id: 1, name: 'Adam Novotný' };
    vi.mocked(usersApi.changeRole).mockResolvedValue({ id: 11, code: 'U011', name: 'Nina New', email: 'nina@example.com', role: 'EDITOR', department: null, active: true, passwordLogin: true });
    renderRoute(<UsersPage />);
    const rows = await screen.findAllByRole('row');
    const me = rows.find((r) => within(r).queryByText('Adam Novotný'))!;
    expect(within(me).getByRole('button', { name: 'Make Reader' })).toBeDisabled();
    const nina = rows.find((r) => within(r).queryByText('Nina New'))!;
    await userEvent.click(within(nina).getByRole('button', { name: 'Make Editor' }));
    await waitFor(() => expect(usersApi.changeRole).toHaveBeenCalledWith(11, 'EDITOR'));
  });
});
