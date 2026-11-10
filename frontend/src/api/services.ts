import { downloadFile, http } from './client';
import type {
  Attachment, Lookups, LoginResponse, Page, Summary, TestDetail, TestFilters, TestRequest, TestSummary, TestType, TestTypeRequest, User,
} from '../models/types';

const clean = (o: Record<string, unknown>) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''));

export interface ListParams extends TestFilters { page: number; size: number; sort?: string }

export const authApi = {
  demoUsers: () => http.get<User[]>('/auth/demo-users').then((r) => r.data),
  login: (userId: number) => http.post<LoginResponse>('/auth/demo-login', { userId }).then((r) => r.data),
  me: () => http.get<User>('/auth/me').then((r) => r.data),
};

export const testsApi = {
  list: (p: ListParams) => http.get<Page<TestSummary>>('/tests', { params: clean({ ...p }) }).then((r) => r.data),
  get: (id: number) => http.get<TestDetail>(`/tests/${id}`).then((r) => r.data),
  summary: () => http.get<Summary>('/tests/summary').then((r) => r.data),
  create: (req: TestRequest, files: File[]) => {
    const fd = new FormData();
    fd.append('data', new Blob([JSON.stringify(req)], { type: 'application/json' }));
    files.forEach((f) => fd.append('files', f));
    return http.post<TestDetail>('/tests', fd).then((r) => r.data);
  },
  update: (id: number, req: TestRequest) => http.put<TestDetail>(`/tests/${id}`, req).then((r) => r.data),
  changeStatus: (id: number, status: string) => http.patch<TestDetail>(`/tests/${id}/status`, { status }).then((r) => r.data),
  exportCsv: (f: TestFilters & { sort?: string }) => downloadFile('/tests/export', clean({ format: 'csv', ...f }), 'technical-tests.csv'),
};

export const attachmentsApi = {
  upload: (testId: number, files: File[]) => {
    const fd = new FormData();
    files.forEach((f) => fd.append('files', f));
    return http.post<Attachment[]>(`/tests/${testId}/attachments`, fd).then((r) => r.data);
  },
  remove: (testId: number, id: number) => http.delete(`/tests/${testId}/attachments/${id}`),
  download: (testId: number, a: Attachment) => downloadFile(`/tests/${testId}/attachments/${a.id}/download`, undefined, a.originalFilename),
};

export const typesApi = {
  list: (activeOnly = false) => http.get<TestType[]>('/test-types', { params: { activeOnly } }).then((r) => r.data),
  create: (r: TestTypeRequest) => http.post<TestType>('/test-types', r).then((x) => x.data),
  update: (id: number, r: TestTypeRequest) => http.put<TestType>(`/test-types/${id}`, r).then((x) => x.data),
  setActive: (id: number, active: boolean) => http.patch<TestType>(`/test-types/${id}/${active ? 'activate' : 'deactivate'}`).then((x) => x.data),
};

export const lookupsApi = { get: () => http.get<Lookups>('/lookups').then((r) => r.data) };
