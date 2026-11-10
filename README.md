# MET-DAT – Technical Tests Management System (benchmark demo)

Demo module for recording technical tests performed on vehicles (MET-DAT Benchmark 2026). **All data is synthetic** (vehicles, people, e-mails, attachments).

| | |
|---|---|
| Stack | Java 21 · Spring Boot 3.4 · Spring Security (JWT bearer) · Spring Data JPA/Hibernate · PostgreSQL 16 · Flyway · springdoc OpenAPI · React 18 + TypeScript + Vite · MUI · React Router · TanStack Query · React Hook Form |
| Delivery | One Docker image (Spring Boot serves the built SPA) + PostgreSQL |
| Tests | 47 backend (unit + integration on real PostgreSQL) · 18 frontend (Vitest) · HTTP smoke test script (30 checks) |

## 1. Features (F01–F12)

| ID | Implemented |
|---|---|
| F01 | Test overview: server-side pagination, sorting, status chips, loading/empty/error states, open detail |
| F02 | Create form (RHF): mandatory VIN, type, date or ISO week, responsible person, status; optional department, result, comment, attachments; client **and** server validation; audit auto-filled |
| F03 | Edit (Editor only); Modified By/At updated by the server, Created By/At preserved; optimistic locking; an already assigned inactive type may stay |
| F04 | Detail page: all fields, attachments (download / upload / delete by role), audit data |
| F05 | Filters VIN (contains), type, status, date From/To – executed in SQL, not in the browser |
| F06 | Completion rule enforced in the backend inside a row-locked transaction (see §6) |
| F07 | Multi-file upload, download, delete; extension + MIME + magic-byte + size validation; safe stored names |
| F08 | Reader / Editor enforced by Spring Security (URL rules **and** `@PreAuthorize`); the role is re-read from the database on every request; tokens are signed by the server. E-mail + password registration/login and demo identities |
| F09 | CSV export of **all** records matching the current filters |
| F10 | Test-type management page (create, edit, activate, deactivate; never deleted) |
| F11 | PostgreSQL schema, Flyway migrations, seed from the workbook, `database/initialization.sql` |
| F12 | This README, docs/, Docker Compose, Render blueprint, VPS compose |

## 2. Quick start

### Option A – Docker (recommended)
```bash
cp .env.example .env        # then edit POSTGRES_PASSWORD and APP_JWT_SECRET (>= 32 chars)
docker compose up --build
```
Open http://localhost:8080 · API docs http://localhost:8080/swagger-ui.html

### Option B – Windows without Docker (what was used for development)
Prerequisites: Node 20+, JDK 21, Maven 3.9 (portable copies can be unpacked into `.tools/`; `scripts/env.ps1` puts them on PATH).
```powershell
.\scripts\run-local.ps1     # builds the frontend, starts backend + a persistent embedded PostgreSQL (./data/pg) on http://localhost:8080
```

### Option C – own PostgreSQL, run backend and frontend separately
```bash
createdb metdat            # any PostgreSQL 14+; Flyway creates schema + seed on first start
cd backend && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/metdat SPRING_DATASOURCE_USERNAME=metdat SPRING_DATASOURCE_PASSWORD=... \
  APP_JWT_SECRET=<32+ chars> mvn spring-boot:run          # http://localhost:8080
cd frontend && npm ci && npm run dev                      # http://localhost:5173 (proxies /api to :8080)
```

### Tests
```bash
cd backend && mvn test        # embedded PostgreSQL, no Docker needed
cd frontend && npm test
node scripts/smoke-test.mjs http://localhost:8080   # black-box check of a running instance
```

## 3. Accounts: registration, login and demo users

* **Register** (tab *Create account*): name, e-mail, password (8–72 chars, stored only as a BCrypt hash). New accounts are **always Readers** – the API has no role field, and a role in the payload is ignored. Disable with `APP_REGISTRATION_ENABLED=false`.
* **Sign in** with e-mail + password. Failed attempts are throttled (5 per 10 minutes per account, HTTP 429).
* **Editors** grant or revoke the Editor role on the *Users* page (`PATCH /api/users/{id}/role`); changes apply immediately because the role is read from the database on each request; you cannot change your own role.
* **Demo users** (below) keep working for evaluators: one click, no password.

### Demo users (fixed, seeded from the workbook)

Open the app → tab **Demo users** → **Quick start as Editor / Reader**, or pick a person. Switch identity any time via the account chip at the top right. Demo users have no password (`POST /api/auth/demo-login {userId}` → server-signed JWT); they cannot be used for password login. Disable with `APP_DEMO_ENABLED=false`.

| Role | Users |
|---|---|
| Editor (6) | Adam Novotný, Barbora Králová, David Černý, Eliška Horáková, Filip Veselý, Hana Němcová |
| Reader (4) | Jakub Dvořák, Klára Procházková, Lukáš Marek, Monika Svobodová |

## 4. Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | localhost / metdat | JDBC settings (alternatively `DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD`) |
| `APP_JWT_SECRET` | random per start | HS256 signing key, ≥ 32 chars. **Set in production** or tokens die on restart |
| `APP_DEMO_ENABLED` | `true` | demo sign-in endpoints |
| `APP_REGISTRATION_ENABLED` | `true` | self-registration (new accounts are Readers) |
| `APP_STORAGE_TYPE` | `filesystem` | `filesystem` (needs persistent volume at `APP_STORAGE_PATH`) or `database` (bytes in PostgreSQL) |
| `APP_UPLOAD_MAX_FILE_SIZE` | `10MB` | per-file limit |
| `APP_UPLOAD_ALLOWED_EXTENSIONS` | `pdf,csv,txt,xlsx,docx,png,jpg,jpeg` | allow-list |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | only needed if the UI is hosted on another origin |
| `PORT` | `8080` | HTTP port |

## 4a. Languages and theme

The UI is available in **English and Czech** (EN/CS switch in the top bar; remembered, defaults to the browser language) and in **light and dark mode** (remembered, defaults to the OS setting). The UI sends `Accept-Language` and the **API answers in the same language** (validation errors, F06 reasons, upload errors): messages live in `backend/src/main/resources/messages*.properties`, UI texts in `frontend/src/i18n/locales/`. Seeded data values (types, comments, statuses/results as stored) are not translated, only their display labels.

## 5. Attachments

Allowed: **PDF, CSV, TXT, XLSX, DOCX, PNG, JPG** (configurable). Max **10 MB per file**, 60 MB per request. A file is accepted only if extension is allowed, the declared MIME type matches the extension, the content starts with the right signature (e.g. `%PDF-`; CSV/TXT must be text), and it is non-empty. Original names are sanitised (directories stripped); files are stored under a server-generated UUID name; the storage layer rejects path traversal. Rejections return HTTP 400 with a readable message shown in the UI. Seeded attachments are **generated placeholder files** labelled `SYNTHETIC DEMO FILE` (the workbook only has paths).

## 6. Business rules

* **F06**: a test can be set to *Completed* (via status change, edit, or on creation) only if its result is exactly **Passed** or **Failed** *and* ≥ 1 attachment exists → otherwise **HTTP 409** (`COMPLETION_REQUIREMENTS_NOT_MET`) with the exact reasons. *Not evaluated*, empty, *Conditionally passed*, *Passed with remarks*, *Preliminary*, *Cancelled* are all rejected.
* Completed can be reopened to Draft / In Progress (any status ↔ any status is allowed).
* The last attachment of a Completed test cannot be deleted (409 `LAST_ATTACHMENT_OF_COMPLETED_TEST`); reopen first.
* Status change, edit and attachment add/delete lock the test row (`SELECT … FOR UPDATE`) so a concurrent delete and complete cannot leave an inconsistent state.
* Inactive test types: not selectable for new assignments (backend rejects with 400); existing records keep and display them.

## 7. API

Swagger UI `/swagger-ui.html`, OpenAPI JSON `/v3/api-docs`. Main endpoints: `GET/POST /api/tests`, `GET/PUT /api/tests/{id}`, `PATCH /api/tests/{id}/status`, `GET /api/tests/export?format=csv`, `…/attachments` (list, upload, `/{id}/download`, delete), `/api/test-types` (+ `/activate`, `/deactivate`), `/api/auth/*`, `/api/lookups`, `/actuator/health`. Errors share one JSON shape `{timestamp,status,code,message,path,fieldErrors[]}`.

## 8. Data notes and interpretations (read before judging the data)

See **docs/seed-data-validation-report.md**. In short: statuses *Planned*/*Cancelled* kept as separate statuses (not mapped to Draft); extra results kept; empty result stays empty; **13 legacy Completed records** violate the F06 rule although the workbook README claims otherwise – imported unchanged, rule not weakened; test dates are ISO calendar weeks (`KT08/26`), filtered by Monday–Sunday overlap; 3 records have Modified < Created.

## 9. Deployment

See **docs/deployment-guide.md** (Render blueprint `render.yaml`, VPS with automatic HTTPS `docker-compose.vps.yml`).

## 10. Known limitations / deviations

* Demo identities have **no passwords** (spec allows a simplified mechanism). Registration has no e-mail verification and no password reset; the login throttle is in-memory (per instance). Not hardened for real personal data.
* Test records cannot be deleted (explicitly out of scope).
* Export is CSV only (comma separated, UTF-8 with BOM); the whole file is built in memory – fine for demo volumes, use streaming for millions of rows.
* No virus scanning of uploads; no object-store (S3) adapter yet (the `FileStorage` interface is ready for one).
* Date filter semantics on calendar weeks are an interpretation (overlap), documented above.
* Frontend uses a single JS bundle (~690 kB, 221 kB gzip), no code splitting.
* Public deployment status: see docs/deployment-guide.md §6.

## 11. Layout
```
backend/   Spring Boot (config, security, domain, repository, dto, mapper, service, storage, web, exception) + Flyway migrations
frontend/  React SPA (pages, components, api, auth, models)
database/  initialization.sql (generated), tools/generate-seed.mjs (workbook -> migrations + report)
docs/      architecture, database-design, effort-estimation, strategic-answers, seed report, demo scenarios, deployment
input/     the two official files (read by the seed generator)
scripts/   env.ps1, run-local.ps1, smoke-test.mjs
```
