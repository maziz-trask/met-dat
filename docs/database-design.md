# Database design (PostgreSQL 16)

Schema: `backend/src/main/resources/db/migration/V1__schema.sql` (also in `database/initialization.sql`).

```
test_type 1───* technical_test 1───* attachment ───(storage_location)──► attachment_content (optional bytea store)
app_user  (identity & role; referenced by e-mail in audit columns, no FK so history survives user changes)
```

| Table | Notes |
|---|---|
| `test_type` | `id`, `name` (unique, case-insensitive), `description`, `active`, audit. **Never deleted** – FK from tests prevents it; deactivation only flips `active`. |
| `app_user` | `name`, `email` (unique ci), `role` ∈ {READER, EDITOR} (CHECK), `department`, `active`, `password_hash` (BCrypt, NULL for seeded demo identities; migration V4). |
| `technical_test` | business id `test_number` (unique, `TEST-yyyy-nnnn`, sequence continues at 51), `vin`, FK `test_type_id`, `department`, `test_year`, `test_week`, optional `test_date`, `period_start/end` (CHECK start ≤ end), `responsible_person`, `status` (CHECK: Draft, Planned, In Progress, Completed, Cancelled), `result` (nullable; CHECK 7 values), `comment`, `created_by/at`, `modified_by/at`, `version`. |
| `attachment` | FK `technical_test_id`, `original_filename`, `stored_filename` (UUID), `content_type`, `file_size`, `storage_location`, `synthetic`, `uploaded_by/at`. |
| `attachment_content` | key → bytes, used only with `APP_STORAGE_TYPE=database`. |

Indexes: `upper(vin)`, `test_type_id`, `status`, `(period_start, period_end)`, `attachment(technical_test_id)`; unique test_number, type name, user e-mail. Audit timestamps are `timestamptz` (UTC; workbook local time Europe/Prague converted on import).

Normalisation: type names live only in `test_type`; tests reference ids. Department and responsible person stay as text (the workbook has no master tables for them; lookups are derived from data/users).

Integration with JPA: `ddl-auto=validate` – Flyway owns the schema; Hibernate only verifies it.
