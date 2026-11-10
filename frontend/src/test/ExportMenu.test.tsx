import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestListPage } from '../pages/TestListPage';
import { lookups, renderRoute, types } from './helpers';

const api = vi.hoisted(() => ({ exportFile: vi.fn(), list: vi.fn() }));
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ user: { id: 7, name: 'R', role: 'READER' }, isEditor: false, loading: false }) }));
vi.mock('../api/services', () => ({
  testsApi: { list: api.list, exportFile: api.exportFile },
  typesApi: { list: vi.fn().mockResolvedValue([]) },
  lookupsApi: { get: vi.fn() },
}));
import { lookupsApi, typesApi } from '../api/services';

beforeEach(() => {
  api.exportFile.mockReset().mockResolvedValue(undefined);
  api.list.mockReset().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  vi.mocked(typesApi.list).mockResolvedValue(types);
  vi.mocked(lookupsApi.get).mockResolvedValue(lookups);
});

describe('Export (F09)', () => {
  it('offers CSV and Excel and exports with the CURRENT filters, regardless of the visible page', async () => {
    renderRoute(<TestListPage />, '/tests', '/tests?status=Completed&vin=0000&page=3&size=10');
    await userEvent.click(await screen.findByRole('button', { name: /export all matching tests/i }));
    expect(screen.getByRole('menuitem', { name: 'CSV (.csv)' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('menuitem', { name: 'Excel (.xlsx)' }));
    await waitFor(() => expect(api.exportFile).toHaveBeenCalledTimes(1));
    const [format, filters] = api.exportFile.mock.calls[0];
    expect(format).toBe('xlsx');
    expect(filters).toMatchObject({ status: 'Completed', vin: '0000' });
    expect(filters).not.toHaveProperty('page');   // the export never depends on the displayed page

    await userEvent.click(screen.getByRole('button', { name: /export all matching tests/i }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'CSV (.csv)' }));
    await waitFor(() => expect(api.exportFile).toHaveBeenLastCalledWith('csv', expect.objectContaining({ status: 'Completed' })));
  });

  it('shows the server error when the export fails', async () => {
    api.exportFile.mockRejectedValue({ response: { status: 500, data: { message: 'Export failed on the server.' } } });
    renderRoute(<TestListPage />, '/tests', '/tests');
    await userEvent.click(await screen.findByRole('button', { name: /export all matching tests/i }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'CSV (.csv)' }));
    expect(await screen.findByText('Export failed on the server.')).toBeInTheDocument();
  });
});
