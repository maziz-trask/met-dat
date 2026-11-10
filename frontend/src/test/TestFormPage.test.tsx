import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestFormPage } from '../pages/TestFormPage';
import { lookups, renderRoute, types } from './helpers';

const api = vi.hoisted(() => ({ create: vi.fn() }));
vi.mock('../api/services', () => ({
  testsApi: { create: api.create, get: vi.fn(), update: vi.fn() },
  typesApi: { list: vi.fn().mockResolvedValue([]) },
  lookupsApi: { get: vi.fn() },
}));
import { lookupsApi, typesApi } from '../api/services';

beforeEach(() => {
  api.create.mockReset();
  vi.mocked(typesApi.list).mockResolvedValue(types);
  vi.mocked(lookupsApi.get).mockResolvedValue(lookups);
});

describe('Create test form', () => {
  it('shows meaningful messages for all mandatory fields and does not call the API', async () => {
    renderRoute(<TestFormPage />, '/tests/new', '/tests/new');
    await userEvent.click(await screen.findByRole('button', { name: /create test/i }));
    expect(await screen.findByText('Vehicle ID / VIN is required')).toBeInTheDocument();
    expect(screen.getByText('Test type is required')).toBeInTheDocument();
    expect(screen.getByText('Department is required')).toBeInTheDocument();
    expect(screen.getByText('Responsible person is required')).toBeInTheDocument();
    expect(screen.getByText('Test date is required')).toBeInTheDocument();
    expect(api.create).not.toHaveBeenCalled();
  });

  it('validates the VIN format', async () => {
    renderRoute(<TestFormPage />, '/tests/new', '/tests/new');
    await userEvent.type(await screen.findByLabelText(/vehicle id/i), 'ABC123IOQ');
    await userEvent.click(screen.getByRole('button', { name: /create test/i }));
    expect(await screen.findByText(/VIN must be exactly 17 letters\/digits/)).toBeInTheDocument();
  });

  it('only offers active test types for a new test', async () => {
    renderRoute(<TestFormPage />, '/tests/new', '/tests/new');
    await userEvent.click(await screen.findByRole('combobox', { name: /test type/i }));
    const list = await screen.findByRole('listbox');
    expect(within(list).getByText('Brake test')).toBeInTheDocument();
    expect(within(list).queryByText(/Legacy road noise/)).not.toBeInTheDocument();
  });

  it('shows the server-side validation / rule message when the API rejects the request', async () => {
    api.create.mockRejectedValue({ response: { status: 409, data: { code: 'COMPLETION_REQUIREMENTS_NOT_MET', message: 'The test cannot be set to Completed: at least one attachment must be uploaded.' } } });
    renderRoute(<TestFormPage />, '/tests/new', '/tests/new');
    await userEvent.type(await screen.findByLabelText(/vehicle id/i), 'TMBTST26A00000099');
    await userEvent.click(screen.getByRole('combobox', { name: /test type/i }));
    await userEvent.click(await screen.findByRole('option', { name: 'Brake test' }));
    await userEvent.type(screen.getByLabelText(/department/i), 'ESW');
    await userEvent.type(screen.getByLabelText(/responsible person/i), 'Adam');
    await userEvent.type(screen.getByLabelText(/test date/i, { selector: 'input[type="date"]' }), '2026-03-11');
    await userEvent.click(screen.getByRole('button', { name: /create test/i }));
    await waitFor(() => expect(api.create).toHaveBeenCalled());
    expect(await screen.findByRole('alert')).toHaveTextContent('at least one attachment must be uploaded');
  });
});
