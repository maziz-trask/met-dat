import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiErrorMessage } from '../api/client';
import { TestDetailPage } from '../pages/TestDetailPage';
import { TestTypesPage } from '../pages/TestTypesPage';
import { checkFiles } from '../utils/format';
import { detail, lookups, renderRoute, types } from './helpers';

const auth = vi.hoisted(() => ({ isEditor: false }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ user: { id: 1, name: 'X', role: auth.isEditor ? 'EDITOR' : 'READER' }, isEditor: auth.isEditor, loading: false }) }));
vi.mock('../api/services', () => ({
  testsApi: { get: vi.fn(), changeStatus: vi.fn() },
  attachmentsApi: { upload: vi.fn(), remove: vi.fn(), download: vi.fn() },
  lookupsApi: { get: vi.fn() },
  typesApi: { list: vi.fn(), create: vi.fn(), update: vi.fn(), setActive: vi.fn() },
}));
import { lookupsApi, testsApi, typesApi } from '../api/services';

beforeEach(() => {
  vi.mocked(testsApi.get).mockResolvedValue(detail);
  vi.mocked(lookupsApi.get).mockResolvedValue(lookups);
  vi.mocked(typesApi.list).mockResolvedValue(types);
  vi.mocked(testsApi.changeStatus).mockReset();
});

const renderDetail = () => renderRoute(<TestDetailPage />, '/tests/:id', '/tests/7');

describe('Role based UI (backend enforces the same rules)', () => {
  it('Reader sees details and can download but has no edit/upload/delete/status controls', async () => {
    auth.isEditor = false;
    renderDetail();
    expect(await screen.findByText('TEST-2026-0007', { selector: 'h1' })).toBeInTheDocument();
    expect(screen.getByText('Legacy road noise')).toBeInTheDocument();
    expect(screen.getByText('inactive type')).toBeInTheDocument();   // inactive type still displayed on history record
    expect(screen.getByRole('button', { name: /download report\.pdf/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /^edit$/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /change status/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete report\.pdf/i })).not.toBeInTheDocument();
    expect(screen.queryByTestId('upload-input')).not.toBeInTheDocument();
  });

  it('Editor sees edit, upload and delete controls; deleting asks for confirmation', async () => {
    auth.isEditor = true;
    renderDetail();
    expect(await screen.findByRole('link', { name: /^edit$/i })).toBeInTheDocument();
    expect(screen.getByTestId('upload-input')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /delete report\.pdf/i }));
    expect(await screen.findByText('Delete attachment?')).toBeInTheDocument();
  });

  it('Test type management is read-only for Reader and editable for Editor', async () => {
    auth.isEditor = false;
    const { unmount } = renderRoute(<TestTypesPage />);
    expect(await screen.findByText('Brake test')).toBeInTheDocument();
    expect(screen.getByText('Legacy road noise')).toBeInTheDocument();   // inactive types listed
    expect(screen.queryByRole('button', { name: /new test type/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /deactivate/i })).not.toBeInTheDocument();
    unmount();
    auth.isEditor = true;
    renderRoute(<TestTypesPage />);
    expect(await screen.findByRole('button', { name: /new test type/i })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Deactivate' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^activate$/i })[0]).toBeInTheDocument();
  });
});

describe('API error handling', () => {
  it('shows the exact reason when the backend rejects a transition to Completed', async () => {
    auth.isEditor = true;
    vi.mocked(testsApi.changeStatus).mockRejectedValue({ response: { status: 409, data: { code: 'COMPLETION_REQUIREMENTS_NOT_MET', message: 'The test cannot be set to Completed: the result must be Passed or Failed (current result: Not evaluated).' } } });
    renderDetail();
    await userEvent.click(await screen.findByRole('button', { name: /change status/i }));
    await userEvent.click(screen.getByRole('combobox', { name: /new status/i }));
    await userEvent.click(await screen.findByRole('option', { name: 'Completed' }));
    await userEvent.click(screen.getByRole('button', { name: /apply/i }));
    await waitFor(() => expect(testsApi.changeStatus).toHaveBeenCalledWith(7, 'Completed'));
    expect((await screen.findAllByText(/current result: Not evaluated/)).length).toBeGreaterThan(0);
  });

  it('apiErrorMessage maps server, auth and network failures', () => {
    expect(apiErrorMessage({ response: { status: 409, data: { message: 'Boom' } } })).toBe('Boom');
    expect(apiErrorMessage({ response: { status: 403, data: {} } })).toMatch(/not allowed/);
    expect(apiErrorMessage({ response: { status: 401, data: null } })).toMatch(/sign in/);
    expect(apiErrorMessage({ response: { status: 500, data: null } })).toMatch(/HTTP 500/);
    expect(apiErrorMessage({ request: {} })).toMatch(/could not be reached/);
  });

  it('client-side file pre-check explains unsupported and oversized files', () => {
    const big = new File([new Uint8Array(20)], 'big.pdf');
    Object.defineProperty(big, 'size', { value: 11 * 1024 * 1024 });
    const problems = checkFiles([new File(['x'], 'virus.exe'), big, new File(['x'], 'ok.csv')], ['pdf', 'csv'], 10 * 1024 * 1024);
    expect(problems).toHaveLength(2);
    expect(problems[0]).toMatch(/unsupported type/);
    expect(problems[1]).toMatch(/too large/);
  });
});
