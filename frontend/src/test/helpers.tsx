import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { NotifyProvider } from '../components/Notify';
import type { TestDetail, TestType } from '../models/types';

export function renderRoute(ui: ReactElement, path = '/', route = '/') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <NotifyProvider>
        <MemoryRouter initialEntries={[route]}>
          <Routes><Route path={path} element={ui} /></Routes>
        </MemoryRouter>
      </NotifyProvider>
    </QueryClientProvider>,
  );
}

export const lookups = {
  statuses: ['Draft', 'Planned', 'In Progress', 'Completed', 'Cancelled'],
  results: ['Passed', 'Failed', 'Not evaluated', 'Preliminary'],
  departments: ['ESW', 'EKB'], responsiblePersons: ['Adam Novotný'], allowedExtensions: ['pdf', 'csv'], maxFileSizeBytes: 10 * 1024 * 1024,
};

export const types: TestType[] = [
  { id: 1, code: 'TT01', name: 'Brake test', description: 'd', active: true, usageCount: 3, createdBy: 'x', createdAt: '2026-01-01T00:00:00Z', modifiedBy: null, modifiedAt: null },
  { id: 11, code: 'TT11', name: 'Legacy road noise', description: null, active: false, usageCount: 0, createdBy: 'x', createdAt: '2026-01-01T00:00:00Z', modifiedBy: null, modifiedAt: null },
];

export const detail: TestDetail = {
  id: 7, testNumber: 'TEST-2026-0007', vin: 'TMBTST26A00000007', testTypeId: 11, testTypeName: 'Legacy road noise', testTypeActive: false, department: 'ESW',
  testDate: null, testWeek: 'KT08/26', periodStart: '2026-02-16', periodEnd: '2026-02-22', responsiblePerson: 'Adam Novotný', status: 'In Progress', result: 'Passed',
  comment: 'hello', attachments: [{ id: 3, originalFilename: 'report.pdf', contentType: 'application/pdf', fileSize: 2048, synthetic: true, uploadedBy: 'seed-import', uploadedAt: '2026-02-06T08:00:00Z' }],
  createdBy: 'a@example.com', createdAt: '2026-02-06T08:00:00Z', modifiedBy: null, modifiedAt: null, version: 0,
};
