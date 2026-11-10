# Builds met-dat-review-package.zip for the customer: source + Docker files + docs + a ready .env + START-HERE.md.
# Usage (PowerShell, project root):  .\scripts\make-review-package.ps1
$ErrorActionPreference = 'Stop'
$root  = Split-Path $PSScriptRoot -Parent
$stage = Join-Path ([IO.Path]::GetTempPath()) 'met-dat-package-stage'
$pkg   = Join-Path $stage 'met-dat'
$out   = Join-Path $root 'dist'
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Force $pkg, $out | Out-Null

# copy the project without tooling, build output, secrets, VCS data and the customer's own input files
robocopy $root $pkg /E /NFL /NDL /NJH /NJS /NP `
  /XD .git .tools node_modules target dist data input static .idea .vscode coverage `
  /XF .env *.log *.tgz *.zip | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy failed ($LASTEXITCODE)" }
$staticDir = Join-Path $pkg 'backend\src\main\resources\static'
if (Test-Path $staticDir) { Remove-Item $staticDir -Recurse -Force }

# package-specific demo secrets (not used anywhere else)
function Rand($n) { -join ((48..57 + 65..90 + 97..122) | Get-Random -Count $n | ForEach-Object { [char]$_ }) }
@"
# Demo-only settings generated for this review package. Safe to edit.
POSTGRES_DB=metdat
POSTGRES_USER=metdat
POSTGRES_PASSWORD=$(Rand 24)
APP_JWT_SECRET=$(Rand 48)
APP_DEMO_ENABLED=true
APP_REGISTRATION_ENABLED=true
APP_STORAGE_TYPE=filesystem
APP_PORT=8080
"@ | Set-Content (Join-Path $pkg '.env') -Encoding ascii

@'
# MET-DAT – Technical Tests Management System (review package)

Demonstration system with **synthetic data**. Everything runs in Docker; nothing else needs to be installed.

## Requirements
* Docker Desktop (Windows/macOS) or Docker Engine + Compose v2 (Linux), running
* ~2 GB free RAM, ~3 GB disk, internet access for the first build (downloads base images and dependencies)
* Port 8080 free (otherwise edit `APP_PORT` in the file `.env`)

## Start (3 commands)

Open a terminal (PowerShell / Terminal) in the unzipped folder `met-dat`:

```
cd met-dat
docker compose up --build -d
docker compose logs -f app
```

The first start takes about 5–10 minutes (build). It is ready when the log shows `Started MetDatApplication`
(press Ctrl+C to leave the log; the application keeps running). Then open:

* **Application:** http://localhost:8080
* **API documentation (Swagger):** http://localhost:8080/swagger-ui.html
* **Health check:** http://localhost:8080/actuator/health

## How to review
1. Open the application → tab **Demo users** → **Quick start as Editor** (full rights) or **Quick start as Reader** (read-only).
   Switch identity any time from the account chip (top right). You can also **create an account** (new accounts are Readers;
   an Editor can promote users on the *Users* page). Language **EN/CS** and **light/dark** mode are in the top bar.
2. A guided walk-through is in `docs/demo-scenarios.md`. Data notes and the 13 inconsistent seed records: `docs/seed-data-validation-report.md`.
3. **Export** to CSV or Excel (XLSX) is in the test overview (button *Export*).
4. Documentation: `README.md`, `docs/architecture.md`, `docs/database-design.md`, `docs/effort-estimation.md`, `docs/strategic-answers.md`.

## Useful commands
```
docker compose ps                 # status of the containers
docker compose logs -f app        # application log
docker compose restart app        # restart the application (data is kept)
docker compose down               # stop (data is kept in Docker volumes)
docker compose down -v            # stop and DELETE all data (database + uploaded files)
```

## Optional checks
* Backend tests (need JDK 21 + Maven, run in `backend/`): `mvn test` – 50 tests on an embedded PostgreSQL.
* Frontend tests (need Node 20+, run in `frontend/`): `npm ci && npm test` – 20 tests.
* Black-box test of the running application (needs Node 18+): `node scripts/smoke-test.mjs http://localhost:8080`

## Troubleshooting
* *"port is already allocated"* → change `APP_PORT` in `.env` (e.g. `8090`) and run `docker compose up -d` again; open that port.
* *Build fails while downloading* → check the internet/proxy settings of Docker Desktop and re-run `docker compose up --build -d`.
* *Page does not load right after start* → the application needs about a minute after the containers start; check `docker compose logs app`.
* Windows: if `docker` is not found, start Docker Desktop first and open a new terminal.

## Notes
* All vehicles, people and e-mail addresses are synthetic. Attachments of the demo records are generated placeholder files labelled "SYNTHETIC DEMO FILE".
* Demo identities have no password by design (simplified identity allowed by the specification). The `.env` file contains demo-only secrets generated for this package.
'@ | Set-Content (Join-Path $pkg 'START-HERE.md') -Encoding utf8

$zip = Join-Path $out 'met-dat-review-package.zip'
if (Test-Path $zip) { Remove-Item $zip -Force }
Push-Location $stage
tar -a -c -f $zip met-dat      # bsdtar writes portable zip paths (Compress-Archive in PS 5.1 uses backslashes)
Pop-Location
Remove-Item $stage -Recurse -Force
Get-Item $zip | Select-Object FullName, @{n='SizeMB';e={[math]::Round($_.Length/1MB,2)}}
