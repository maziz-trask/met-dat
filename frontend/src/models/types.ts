export type Role = 'READER' | 'EDITOR';

export interface User { id: number; code: string; name: string; email: string; role: Role; department: string | null }
export interface UserSummary extends User { active: boolean; passwordLogin: boolean }
export interface LoginResponse { token: string; expiresAt: string; user: User }
export interface AuthConfig { demoEnabled: boolean; registrationEnabled: boolean }
export interface Credentials { email: string; password: string }
export interface Registration extends Credentials { name: string }

export interface Attachment {
  id: number; originalFilename: string; contentType: string; fileSize: number; synthetic: boolean; uploadedBy: string; uploadedAt: string;
}

interface TestCommon {
  id: number; testNumber: string; vin: string; testTypeId: number; testTypeName: string; testTypeActive: boolean; department: string;
  testDate: string | null; testWeek: string; periodStart: string; periodEnd: string; responsiblePerson: string;
  status: string; result: string | null;
}
export interface TestSummary extends TestCommon { attachmentCount: number }
export interface TestDetail extends TestCommon {
  comment: string | null; attachments: Attachment[]; createdBy: string; createdAt: string; modifiedBy: string | null; modifiedAt: string | null; version: number;
}

export interface TestRequest {
  vin: string; testTypeId: number; department: string; testDate: string | null; testWeek: string | null;
  responsiblePerson: string; status: string; result: string | null; comment: string | null; version?: number;
}

export interface TestType {
  id: number; code: string; name: string; description: string | null; active: boolean; usageCount: number;
  createdBy: string; createdAt: string; modifiedBy: string | null; modifiedAt: string | null;
}
export interface TestTypeRequest { name: string; description: string | null; active?: boolean }

export interface Lookups {
  statuses: string[]; results: string[]; departments: string[]; responsiblePersons: string[]; allowedExtensions: string[]; maxFileSizeBytes: number;
}
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface Summary { total: number; byStatus: Record<string, number> }

export interface TestFilters { vin?: string; testTypeId?: string; status?: string; dateFrom?: string; dateTo?: string }

export interface ApiErrorBody {
  status: number; code: string; message: string; path: string; fieldErrors?: { field: string; message: string }[];
}
