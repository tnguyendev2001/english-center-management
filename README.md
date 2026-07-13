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
