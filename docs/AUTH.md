# Authentication & Authorization (V1)

## Overview

- Stateless JWT access tokens (`Authorization: Bearer <token>`)
- Spring Security OAuth2 Resource Server (Nimbus HS256)
- Roles: `ADMIN`, `TEACHER`, `STUDENT` (exactly one role per account)
- No refresh tokens, no public registration, no MFA

## Environment variables

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | _(required in prod)_ | HS256 secret, min 32 bytes. Local profile has a documented default. |
| `JWT_EXPIRATION_MINUTES` | `240` | Access token lifetime |
| `JWT_ISSUER` | `school-management` | Validated on every request |
| `JWT_AUDIENCE` | `school-management-web` | Validated on every request |
| `APP_ALLOWED_ORIGINS` | `http://localhost:5173` | CORS allow-list (comma-separated). Do not use `*` in production. |
| `APP_ADMIN_USERNAME` | _(empty)_ | Bootstrap first ADMIN when none exists |
| `APP_ADMIN_PASSWORD` | _(empty)_ | Bootstrap password; must satisfy password policy |

CSRF remains disabled because auth uses the Authorization header, not cookies. Revisit CSRF if cookie-based auth is introduced later.

## Bootstrap first ADMIN

1. Ensure no ADMIN account exists.
2. Set `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`.
3. Start the backend.
4. Login, then change password immediately (`mustChangePassword=true`).

Bootstrap is idempotent and never overwrites an existing ADMIN.

## Manual test checklist

1. Bootstrap ADMIN and login.
2. Create Teacher profile, then TEACHER account; copy one-time password.
3. Create STUDENT account from an existing student; copy one-time password.
4. Login as TEACHER/STUDENT → forced change-password.
5. After password change, old token is rejected; new login works.
6. ADMIN menu includes Finance / Import / User management.
7. TEACHER sees assigned classrooms only; other classroom IDs return 403.
8. STUDENT uses `/api/me/*` only; cannot access Finance.
9. Disable account → active token stops working.
10. Reset password → tokenVersion increments; old token rejected.
11. Expired/invalid token → frontend returns to Login (401).
12. Forbidden route → `/forbidden` (403), session kept.
13. CORS works for local Vite and deployed frontend origin.

## Teacher ownership

Classrooms have optional `teacher_id`. Ownership checks use that FK. Existing classrooms keep `teacher_name` for display; assign `teacherId` when linking a Teacher profile.
