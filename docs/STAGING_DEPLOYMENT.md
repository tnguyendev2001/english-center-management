# Staging Deployment Guide

English Center Management — deploy staging/test environment on **Vercel (frontend)**, **Render (backend)**, and **Neon (PostgreSQL)**.

Branch: `develop`  
Monorepo layout:

```text
backend/    Spring Boot API (Docker on Render)
frontend/   React + Vite SPA (Vercel)
```

---

## A. What is already prepared in this repository

| Area | Status |
|------|--------|
| PostgreSQL driver + Flyway PostgreSQL | `backend/pom.xml` |
| Flyway migrations (PostgreSQL syntax) | `backend/src/main/resources/db/migration/` |
| Spring profiles | `application.yml`, `application-local.yml`, `application-prod.yml` |
| Local PostgreSQL | `docker-compose.yml` |
| Backend Docker image | `backend/Dockerfile`, `backend/.dockerignore` |
| Render Blueprint | `render.yaml` (root) |
| Vercel SPA routing | `frontend/vercel.json` |
| Health endpoint | `/actuator/health` (Spring Boot Actuator) |
| CORS from env | `CORS_ALLOWED_ORIGINS` → `SecurityConfig` |
| Frontend API URL | `VITE_API_BASE_URL` → `frontend/src/api/apiConfig.ts` |
| Env examples | `backend/.env.example`, `frontend/.env.example` |

### Spring profiles

| Profile | File | Use case |
|---------|------|----------|
| `local` (default) | `application-local.yml` | Local dev + Docker PostgreSQL |
| `prod` | `application-prod.yml` | Render staging (`SPRING_PROFILES_ACTIVE=prod`) |

Hibernate: `ddl-auto: validate` — schema is managed by Flyway only.

### API base path

All REST controllers are under `/api/*`. Frontend must set:

```text
VITE_API_BASE_URL=https://YOUR_RENDER_HOST/api
```

---

## B. Manual steps you must do

### Step 1: Review and push to GitHub

```bash
git status
git diff
git add .
git commit -m "prepare staging deployment with PostgreSQL"
git push -u origin develop
```

Do **not** commit `.env`, `.env.local`, or any file containing real passwords/tokens.

---

### Step 2: Create Neon PostgreSQL database

1. Sign in to [Neon](https://neon.tech).
2. Create project: `school-management-staging`.
3. Engine: **PostgreSQL** (16.x).
4. Region: closest to Vietnam (e.g. Singapore / AWS ap-southeast-1 if available).
5. Open **Connect** and copy connection details.

Map Neon values to Render environment variables:

| Render env var | Neon source |
|----------------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL with SSL, e.g. `jdbc:postgresql://HOST/DB?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

Neon often provides a pooled URL — use the **direct JDBC** connection string for Spring Boot + Flyway unless you configure pooler explicitly.

Optional: connect with DBeaver after first backend deploy to verify Flyway created all tables.

---

### Step 3: Deploy backend on Render

#### Option A — Blueprint (recommended)

1. Render Dashboard → **Blueprints** → **New Blueprint Instance**.
2. Connect GitHub repo `english-center-management`.
3. Select branch `develop`.
4. Render reads `render.yaml` at repo root.
5. When prompted, enter secret env vars:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `CORS_ALLOWED_ORIGINS` (temporary: `http://localhost:5173` until Vercel URL is known)
6. Deploy.

#### Option B — Manual Web Service

| Setting | Value |
|---------|-------|
| Type | Web Service |
| Repository | your GitHub repo |
| Branch | `develop` |
| Root Directory | *(leave empty — Docker context is `./backend`)* |
| Runtime | **Docker** |
| Dockerfile path | `./backend/Dockerfile` |
| Docker context | `./backend` |
| Plan | Free |
| Health Check Path | `/actuator/health` |
| Auto-Deploy | Yes (on `develop`) |

Environment variables:

| Key | Value | Secret? |
|-----|-------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` | No |
| `SPRING_DATASOURCE_URL` | Neon JDBC URL | **Yes** |
| `SPRING_DATASOURCE_USERNAME` | Neon user | **Yes** |
| `SPRING_DATASOURCE_PASSWORD` | Neon password | **Yes** |
| `CORS_ALLOWED_ORIGINS` | see Step 5 | No |
| `DB_MAX_POOL_SIZE` | `5` | No |
| `DB_MIN_IDLE` | `0` | No |

After deploy, copy backend URL, e.g.:

```text
https://school-management-backend-staging.onrender.com
```

Verify health:

```text
https://YOUR_BACKEND.onrender.com/actuator/health
```

Expected: `{"status":"UP"}`

---

### Step 4: Deploy frontend on Vercel

1. Vercel → **Add New Project** → import same GitHub repo.
2. **Root Directory:** `frontend`
3. **Framework Preset:** Vite
4. **Build Command:** `npm run build`
5. **Output Directory:** `dist`
6. **Production Branch:** `develop`
7. Environment variable:

| Key | Value |
|-----|-------|
| `VITE_API_BASE_URL` | `https://YOUR_RENDER_BACKEND.onrender.com/api` |

8. Deploy.
9. Copy frontend URL, e.g. `https://school-management-staging.vercel.app`

**Important:** changing `VITE_API_BASE_URL` on Vercel requires a **redeploy** (env vars are baked in at build time).

---

### Step 5: Update CORS on Render

After you have the real Vercel URL, update Render env var:

```text
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://YOUR_FRONTEND.vercel.app
```

Save → Render redeploys backend automatically.

Optional: set `FRONTEND_URL=https://YOUR_FRONTEND.vercel.app` on Render.

---

### Step 6: Smoke tests

| # | Test | Expected |
|---|------|----------|
| 1 | `GET /actuator/health` | `UP` |
| 2 | Open Vercel app | Dashboard loads |
| 3 | Browser DevTools → Network | No requests to `localhost` |
| 4 | Student CRUD | Works |
| 5 | Classroom CRUD | Works |
| 6 | Tuition package | Works |
| 7 | Enrollment | Works |
| 8 | Invoice / Payment / Debt | Works |
| 9 | Class session / Attendance | Works |
| 10 | Package change | Works |
| 11 | Direct URL refresh `/students` | No 404 (Vercel SPA rewrite) |
| 12 | Redeploy backend | Data persists (Neon + Flyway) |

---

## C. Daily deploy workflow

```bash
git checkout develop
git pull
# ... make changes ...
git add .
git commit -m "describe the change"
git push origin develop
```

- **Render** auto-deploys when `backend/**` or `render.yaml` changes (per blueprint `buildFilter`).
- **Vercel** auto-deploys when `frontend/**` changes.
- Staging URLs stay the same.
- Do not push unfinished work to `develop`.

---

## D. Rollback

| Component | Action |
|-----------|--------|
| Render backend | Dashboard → Service → **Rollback** to previous deploy |
| Vercel frontend | Dashboard → Deployments → **Promote** previous deployment |
| Database | **Do not** drop tables manually. Flyway is forward-only. Fix forward with a new migration. |

---

## Local development (reference)

```bash
# PostgreSQL
docker compose up -d postgres

# Backend
cd backend
./mvnw spring-boot:run
# profile local is default; uses jdbc:postgresql://localhost:5432/school_management

# Frontend
cd frontend
npm install
npm run dev
# uses frontend/.env.development → http://localhost:8080/api
```

---

## Environment variable reference

| Variable | Local | Render | Vercel | Secret |
|----------|-------|--------|--------|--------|
| `SPRING_PROFILES_ACTIVE` | `local` (default) | `prod` | — | No |
| `SPRING_DATASOURCE_URL` | default localhost | Neon JDBC | — | **Yes** (prod) |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Neon user | — | **Yes** (prod) |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Neon password | — | **Yes** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | localhost + Vercel URL | — | No |
| `VITE_API_BASE_URL` | `.env.development` | — | Render URL + `/api` | No |
| `PORT` | `8080` | set by Render | — | No |
| `DB_MAX_POOL_SIZE` | `5` | `5` | — | No |
| `DB_MIN_IDLE` | `1` | `0` | — | No |
| `FRONTEND_URL` | — | Vercel URL (optional) | — | No |

See also: `backend/.env.example`, `frontend/.env.example`.
