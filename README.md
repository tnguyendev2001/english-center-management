# English Center Management

School management system for a small English center — students, classrooms, enrollment, invoices, payments, attendance, and reports.

## Tech stack

- **Backend:** Java 21, Spring Boot, Spring Data JPA, Flyway, PostgreSQL 16
- **Frontend:** React, TypeScript, Vite, Ant Design, TanStack Query

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker (for local PostgreSQL)

## Local setup

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

Database: `school_management`  
User / password: `postgres` / `postgres`  
Port: `5432`

### 2. Backend

Set `ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`, and `JWT_SECRET` in the shell as
described in [Authentication](#authentication), then run:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API runs at `http://localhost:8080`.

Flyway applies migrations on startup. Hibernate validates the schema (`ddl-auto: validate`).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

## Authentication

The application has one environment-configured administrator. `POST
/api/auth/login` verifies the configured username and BCrypt hash, then issues
an HS256 JWT access token. The frontend keeps the token in `sessionStorage` and
sends it in the `Authorization: Bearer` header. Spring Security validates the
signature, expiry, issuer, and `ADMIN` role for every `/api/**` request.
`/actuator/health` and the login endpoint are the only public endpoints.

Required backend environment variables:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH=<bcrypt-hash>
JWT_SECRET=<base64-random-secret>
JWT_EXPIRATION_MINUTES=480
JWT_ISSUER=school-management-api
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

The frontend requires only:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Generate a BCrypt hash locally from `backend/` without putting the password in
shell history:

```powershell
$securePassword = Read-Host "Admin password" -AsSecureString
$credential = [pscredential]::new("unused", $securePassword)
$env:ADMIN_PASSWORD_TO_HASH = $credential.GetNetworkCredential().Password
try { .\mvnw.cmd -q "-Dtest=AdminPasswordHashGeneratorTest" test } finally { Remove-Item Env:ADMIN_PASSWORD_TO_HASH }
```

Copy the printed BCrypt hash into `ADMIN_PASSWORD_HASH`. Generate a JWT secret
containing 32 cryptographically random bytes:

```bash
python -c "import secrets,base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

Set the backend variables in the terminal before local startup. On Render, set
all backend variables above alongside the existing PostgreSQL variables and use
the exact Vercel origin for `CORS_ALLOWED_ORIGINS`. On Vercel, set only
`VITE_API_BASE_URL` to the Render URL ending in `/api`; never add the JWT secret,
password hash, or database credentials to Vercel.

Logout removes the token from `sessionStorage`; there is no refresh token or
server session. An issued token remains usable until it expires because this
stateless design has no server-side revocation list. To change the admin
password, generate a new BCrypt hash, update `ADMIN_PASSWORD_HASH`, and redeploy.
To rotate credentials immediately, generate a new random secret, update
`JWT_SECRET`, and redeploy. Secret rotation intentionally invalidates all
previously issued tokens.

## Configuration

| Profile | File | Purpose |
|---------|------|---------|
| `local` | `application-local.yml` | Local dev (PostgreSQL on localhost) |
| `prod` | `application-prod.yml` | Production (`DATABASE_URL`, etc.) |

## Database

This project uses **PostgreSQL 16 only**. See `.cursor/rules/database.mdc` and `docs/04-database-convention.md`.

## Staging deployment

See [docs/STAGING_DEPLOYMENT.md](docs/STAGING_DEPLOYMENT.md) for Vercel + Render + Neon setup.

Quick local stack:

```bash
docker compose up -d postgres
cd backend && ./mvnw spring-boot:run
cd frontend && npm run dev
```

## Tests

```bash
cd backend
mvn test
```

Unit tests do not require a running database.
