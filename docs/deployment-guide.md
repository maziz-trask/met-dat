# Deployment guide

## 1. Hosting options evaluated (researched 2026-11-10; prices change – re-check before paying)

| Option | Managed PostgreSQL | Persistent files | HTTPS | Cost / caveats |
|---|---|---|---|---|
| **Render** (recommended for a quick public demo) | yes (free plan: 1 GB, **expires after 30 days**; paid ≈ from $6–7/mo) | free web services have an **ephemeral disk**; disks only on paid services (≈ $0.25/GB-month) | automatic | Free web service sleeps after ~15 min idle (cold start 30–60 s) |
| Railway | yes, one-click | volumes | automatic | no real free tier (trial credit / ≈$5 minimum per month, sources disagree) |
| Fly.io | self-managed Postgres or Managed Postgres (≈ $38/mo quoted) | volumes | automatic | free allowance unclear; more ops work |
| Koyeb | serverless Postgres | – | automatic | free offering reported inconsistently |
| **VPS + Docker Compose + Caddy** | self-managed container | local volume | Let's Encrypt via Caddy | ≈ €4–6/mo, full control, no expiry |

**Recommendation:** Render blueprint with `APP_STORAGE_TYPE=database`. Attachments are stored in the PostgreSQL database that Render persists, so no paid disk is needed, and everything (app + DB + HTTPS) is created from one file (`render.yaml`). Trade-offs: the free DB expires after 30 days and the app sleeps when idle – for a longer-lived review upgrade the database plan (user approval needed, I will not create paid resources) or use the VPS option. Sources: render.com pricing/docs pages as summarised by search on 2026-11-10 (not all verifiable from the official page – confirm at https://render.com/pricing).

## 2. What you need to do (I cannot create accounts or enter credentials for you)

1. Push the project to a **private GitHub repo** (`.env`, `.tools`, `data` are git-ignored):
   ```bash
   cd %USERPROFILE%\Projects\met-dat
   git init && git add . && git commit -m "MET-DAT demo"
   git branch -M main && git remote add origin https://github.com/<you>/met-dat.git && git push -u origin main
   ```
2. Create a Render account, connect GitHub. **Dashboard → New → Blueprint →** pick the repo. Render reads `render.yaml`, creates `met-dat` (web) and `met-dat-db` (free PostgreSQL), generates `APP_JWT_SECRET`, injects the DB settings.
3. Wait for the first deploy (Docker build ≈ 6–10 min). Public URL: `https://met-dat.onrender.com` (or the name Render assigns).
4. Verify: `node scripts/smoke-test.mjs https://<your-url>` (all checks must pass; it adds one record labelled `[SMOKE TEST]`).
5. Persistence check: Dashboard → *Manual Deploy → Restart*, reload the app; the data (including uploaded files) is still there.

## 3. Alternative: VPS with automatic HTTPS
```bash
# on a fresh Ubuntu VPS with Docker; DNS A-record demo.example.com -> server IP
git clone <repo> && cd met-dat
cp .env.example .env && nano .env      # POSTGRES_PASSWORD, APP_JWT_SECRET, DOMAIN=demo.example.com
docker compose -f docker-compose.vps.yml up -d --build
```
Attachments use the `attachments` Docker volume; back up the `pgdata` and `attachments` volumes.

## 4. Production notes
* Secrets only as environment variables (never committed). Rotate `APP_JWT_SECRET` to invalidate sessions.
* Health: `/actuator/health` (and `/liveness`, `/readiness`). Flyway migrates on start-up.
* Demo mode has no passwords: expose only demo/synthetic data, or set `APP_DEMO_ENABLED=false` and add a real identity provider.
* CORS is empty by default in the Render blueprint (same origin).

## 5. Local verification performed (2026-11-10)
Backend 39/39 and frontend 10/10 automated tests green; stack run locally on PostgreSQL 16.4 and `scripts/smoke-test.mjs` passed all 25 checks (reader/editor permissions, upload/download, F06, export, audit), app restarted with data persisted. Docker image build and Compose were **not** executed on this PC (Docker is not installed here), so the Dockerfile/compose/render files are unverified.

## 6. Public deployment status
**Not deployed yet.** Deployment needs your Render/GitHub account and an explicit decision about cost; follow §2. Remaining actions: steps 1–5 above. Once you have the URL, run the smoke test against it to record verification.

## 7. Deploying from a self-hosted GitLab (CI builds the image, Render runs it)
Render cannot clone from a self-hosted GitLab, so the pipeline in `.gitlab-ci.yml` builds the image (Kaniko, no Docker daemon) and pushes `docker.io/<user>/met-dat:latest` to Docker Hub.

1. Docker Hub: create an account and a **Read & Write access token**. (The image is public if the repository is public; it contains only synthetic demo data and no secrets.)
2. GitLab project > Settings > CI/CD > Variables: add `DOCKERHUB_USER` and `DOCKERHUB_TOKEN` (**masked**). Push to `main`; the `build-image` job publishes the image (needs a runner with the Docker/Kubernetes executor).
3. Render > **New > PostgreSQL** (Free), name `met-dat-db`, db `metdat`, user `metdat`.
4. Render > **New > Web Service > Existing Image**: `docker.io/<user>/met-dat:latest`, instance type **Free**, health check path `/actuator/health/liveness`. Environment variables:

| Key | Value |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | from the database's **Internal** connection info |
| `APP_JWT_SECRET` | random string >= 32 chars |
| `APP_STORAGE_TYPE` | `database` |
| `APP_DEMO_ENABLED` | `true` |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=65 -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1` |

5. Free instances have 512 MB RAM: if the service is killed for memory, upgrade the instance type. Updating: push to `main`, then **Manual Deploy > Deploy latest reference** in Render.
