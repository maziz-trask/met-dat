# Demo scenarios (≈15 minutes)

Start: open the app → **Quick start as Reader**.

**A. Reader (read-only)**
1. Overview: sort by *Test date*, page through, filter *Status = Completed* (28 hits), *VIN contains `0000001`*, date range 2026-02-16 → 2026-02-22 (week KT08/26 matches), reset.
2. Open `TEST-2026-0004`: audit data, two attachments, download the PDF (yellow *synthetic* badge; content says SYNTHETIC DEMO FILE).
3. Note: no *New test*, *Edit*, upload, delete or status controls; *Test types* is read-only. Try the API directly in Swagger with a Reader token → 403.
4. **Export** → *CSV* and *Excel (.xlsx)* from the filtered list → both files contain all 28 matching tests, not just one page (open the .xlsx in Excel: bold frozen header, filter buttons).

**B. Switch to Editor** (top-right chip → Adam Novotný)
5. *Create Test*: submit empty form → validation messages; invalid VIN `ABC` → message; valid VIN `TMBTST26A00000099`, type Brzdová zkouška, date, person, status **In Progress**, result *Not evaluated*. Create.
6. Detail → *Change status* → **Completed** → rejected with the exact reason (result must be Passed/Failed; attachment required).
7. Edit → result *Passed* → still rejected (no attachment) → upload a PDF → *Change status* → Completed succeeds. Audit "Modified by/at" updated, Created unchanged.
8. Delete the only attachment → rejected (last attachment of Completed). *Change status → In Progress* → delete works (confirmation dialog).
9. Upload `x.exe` → clear error. Upload an 11 MB file → clear error.
10. *Test types*: deactivate *Osvětlení a viditelnost* (8 tests use it) → open one of those tests: type still shown with *inactive type* badge; edit keeps it; *Create Test* no longer offers it; reactivate.
11. Legacy data: open `TEST-2026-0002` (Completed, *Conditionally passed*) – a documented data exception; move it to Draft, then try Completed → rejected.
12. Open `/swagger-ui.html`, `/actuator/health`.
