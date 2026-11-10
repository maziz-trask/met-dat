import { downloadFile, http } from './client';
import type {
  Attachment, AuthConfig, Credentials, Lookups, LoginResponse, Page, Registration, Role, Summary, TestDetail, TestFilters, TestRequest, TestSummary,
  TestType, TestTypeRequest, User, UserSummary,
} from '../models/types';

const withoutEmpty = (o: Record<string, unknown>) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''));

export interface ListParams extends TestFilters { page: number; size: number; sort?: string }

export const authApi = {
  config: () => http.get<AuthConfig>('/auth/config').then((r) => r.data),
  demoUsers: () => http.get<User[]>('/auth/demo-users').then((r) => r.data),
  demoLogin: (userId: number) => http.post<LoginResponse>('/auth/demo-login', { userId }).then((r) => r.data),
  login: (c: Credentials) => http.post<LoginResponse>('/auth/login', c).then((r) => r.data),
  register: (r: Registration) => http.post<LoginResponse>('/auth/register', r).then((x) => x.data),
  me: () => http.get<User>('/auth/me').then((r) => r.data),
};

export const usersApi = {
  list: () => http.get<UserSummary[]>('/users').then((r) => r.data),
  changeRole: (id: number, role: Role) => http.patch<UserSummary>(`/users/${id}/role`, { role }).then((r) => r.data),
};

export const testsApi = {
  list: (p: ListParams) => http.get<Page<TestSummary>>('/tests', { params: withoutEmpty({ ...p }) }).then((r) => r.data),
  get: (id: number) => http.get<TestDetail>(`/tests/${id}`).then((r) => r.data),
  summary: () => http.get<Summary>('/tests/summary').then((r) => r.data),
  create: (req: TestRequest, files: File[]) => {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(req)], { type: 'application/json' }));
    files.forEach((f) => form.append('files', f));
    return http.post<TestDetail>('/tests', form).then((r) => r.data);
  },
  update: (id: number, req: TestRequest) => http.put<TestDetail>(`/tests/${id}`, req).then((r) => r.data),
  changeStatus: (id: number, status: string) => http.patch<TestDetail>(`/tests/${id}/status`, { status }).then((r) => r.data),
  exportCsv: (f: TestFilters & { sort?: string }) => downloadFile('/tests/export', withoutEmpty({ format: 'csv', ...f }), 'technical-tests.csv'),
};

export const attachmentsApi = {
  upload: (testId: number, files: File[]) => {
    const form = new FormData();
    files.forEach((f) => form.append('files', f));
    return http.post<Attachment[]>(`/tests/${testId}/attachments`, form).then((r) => r.data);
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
