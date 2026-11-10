# Architecture

Modular monolith: one Spring Boot application, one PostgreSQL database, one React SPA that is built and served by the backend (same origin → no CORS in production, a single HTTPS endpoint).

```
Browser (React SPA) ──HTTPS──► Spring Boot ──JDBC──► PostgreSQL (schema + attachments if storage=database)
                                  │  └─ FileStorage ─► filesystem volume | PostgreSQL bytea
                                  └─ /swagger-ui.html, /actuator/health
```

## Backend layers (`cz.metdat`)
| Package | Responsibility |
|---|---|
| `web` | REST controllers, `GlobalExceptionHandler` (uniform `ApiError`) |
| `service` | business logic + transactions: `TechnicalTestService`, `AttachmentService`, `TestTypeService`, `ExportService`, `CompletionRule` (F06), `IsoWeeks`, `AuditStamper`, `TestSpecifications` (filters → SQL) |
| `repository` | Spring Data JPA, row-lock query `findByIdForUpdate` |
| `domain` | JPA entities and enums (status/result persisted as readable labels via converters, guarded by CHECK constraints) |
| `dto`, `mapper` | request/response records; entity↔DTO conversion (entities never leave the service layer) |
| `security`, `config` | JWT issue/verify, filter chain, CORS, OpenAPI, SPA hosting |
| `storage` | `FileStorage` (filesystem or database), `AttachmentValidator`, synthetic demo file generator |

## Key decisions
0. **Authentication**: e-mail/password (BCrypt) with registration (always Reader), plus fixed demo identities. Every token is server-signed; the caller's role is **re-read from the database on each request**, so promotions/demotions/deactivation apply at once. Failed logins are throttled and unknown e-mails cost the same time as wrong passwords.
0a. **Internationalisation**: exceptions carry message *keys*; `GlobalExceptionHandler` resolves them from `messages_{en,cs}.properties` using Accept-Language (bean-validation messages use the same bundles). The React app uses i18next with typed, parity-tested locale files, and a light/dark theme factory.
1. **Server-verified identity**: demo login looks the user up and signs a JWT (`role` from DB). Writes are denied for non-Editors by URL rule *before* body validation, plus `@PreAuthorize` on each write method. Tokens are bearer headers, so CSRF does not apply; no cookies.
2. **F06 in one place**: `CompletionRule.check` is called from create, update and status change; all take a pessimistic row lock, as does attachment deletion, which makes "complete" vs "delete last attachment" serialisable. `@Version` adds optimistic locking for edit forms.
3. **Audit** is stamped only by the server (`AuditStamper`) from the token subject and server clock; DTOs contain no writable audit fields.
4. **Attachments** keep metadata + link in `attachment` (FK to test) so the F06 count is a SQL count. Bytes live behind `FileStorage`; `database` mode makes them persistent on hosts with ephemeral disks. Files written in a transaction that rolls back are removed; deleted files are removed after commit.
5. **Calendar weeks**: `test_year`, `test_week` (as supplied), `period_start/end` (Mon–Sun) for range queries, `test_date` only when a user entered an exact date.
6. **Reference data is data**: test types, statuses, results, departments, persons, upload limits come from `/api/lookups` and `/api/test-types`; nothing is hard-coded in React.
7. **Seeds via Flyway** (`V2`, `V3` generated from the workbook by `database/tools/generate-seed.mjs`); synthetic attachment bytes are created idempotently on start-up.

## Frontend
`pages/` (Dashboard, TestList, TestForm, TestDetail, TestTypes, Login, About) · `components/` (Layout with demo banner and role switcher, chips, dialogs, feedback) · `api/` (axios client, typed services, single error-message mapper) · `auth/` (context; cache is cleared when the identity changes) · `models/` (TypeScript interfaces). List filters/sort/page live in the URL query string (shareable, back-button friendly).
