# Effort estimation (1 MD = 8 working hours of one person)

Estimate for a competent full-stack developer building **this demo module** (analysis, development, testing, documentation, demo environment) **without** counting the strategic document 3.1–3.4. Shared work is listed separately and is **not** repeated in the functions.

| ID | Function | MD | Scope covered | Assumptions / limits |
|---|---|---:|---|---|
| F01 | Test overview | 1.0 | list endpoint, paging, sorting, status/result chips, loading/empty/error states, navigation to detail | server-side paging; no column configuration |
| F02 | Create record | 1.5 | form, client+server validation (VIN pattern, mandatory fields), week/date input, files on create, audit stamping | ISO week input as an addition to date |
| F03 | Edit record | 0.75 | prefilled form, PUT, audit update, optimistic lock, inactive type kept | reuses F02 form |
| F04 | Detail | 1.0 | all fields, audit, attachment panel, status dialog, role-dependent actions | |
| F05 | Search & filter | 1.0 | JPA specifications (VIN, type, status, week-overlap date range), URL-synced filters | calendar-week interpretation documented |
| F06 | Status validation | 1.5 | CompletionRule, row locking, last-attachment rule, 409 error contract, UI messaging, dedicated tests | concurrency tested logically, no load test |
| F07 | Attachments | 2.0 | upload/download/delete, validation (ext/MIME/magic/size), storage abstraction (fs + DB), rollback cleanup, synthetic files | no virus scan, no S3 |
| F08 | Permissions | 1.5 | JWT demo login, role claim, URL + method security, JSON 401/403, role switcher UI, authorization tests | no real IAM/SSO |
| F09 | Export | 0.5 | CSV all-matches, escaping, BOM, formula-injection guard | CSV only, in-memory |
| F10 | Test type management | 1.25 | CRUD API, activate/deactivate, usage counts, UI dialog, rules for inactive types | |
| F11 | Data model & SQL | 1.5 | schema, constraints, indexes, Flyway, workbook importer, validation report, initialization.sql | one-time analysis of data inconsistencies included |
| F12 | Documentation & demo env | 1.5 | README, architecture/DB docs, demo scenarios, deployment files, smoke test | public hosting account set-up by owner not included |
| | **Subtotal functions** | **15.0** | | |

| Shared / cross-cutting (not in functions) | MD | Content |
|---|---:|---|
| Project setup | 0.5 | repo, Maven/Vite, tooling, .gitignore |
| Backend common architecture | 1.0 | layering, exception handling, OpenAPI, Jackson/JPA config, audit stamper |
| Frontend shell & shared components | 1.5 | layout, routing, API client, auth context, theme, notifications, chips, dialogs |
| Environment & Docker | 0.75 | Dockerfile, Compose, profiles, env configuration |
| CI/CD | 0.5 | pipeline: build, test, image |
| Test infrastructure | 0.5 | embedded PostgreSQL base class, frontend test setup |
| **Subtotal shared** | **4.75** | |
| **TOTAL** | **19.75 MD** | |

Notes: figures are a professional estimate for manual development; the actual implementation here was AI-assisted and took far less elapsed time, which is not a basis for the estimate. Contingency (≈15 %) is not included. CI/CD is estimated but a pipeline file is not part of the delivered repository.
