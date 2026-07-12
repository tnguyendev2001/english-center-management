# 07 - Development Phases

Do not build the whole app at once.

Implement one phase at a time.

## Phase 0 - Project foundation

Goal:
- Create runnable backend and frontend.
- Set up common conventions.
- Set up docs and Cursor rules.

Backend:
- Spring Boot project.
- application.yml.
- PostgreSQL connection.
- Flyway enabled.
- Common ApiResponse.
- PageResponse.
- GlobalExceptionHandler.
- BusinessException.
- Basic CORS.
- Simple security placeholder.

Frontend:
- React Vite project.
- Ant Design installed.
- React Router.
- TanStack Query provider.
- Axios httpClient.
- AppLayout.
- Placeholder pages.

Do not implement business modules in Phase 0.

## Phase 1 - Master data

Modules:
- Student
- TuitionPackage
- Classroom
- ClassPackage

Goal:
- Manage students.
- Manage tuition packages.
- Manage classrooms.
- Allow one classroom to have many tuition packages.

Must have:
- CRUD.
- Search/filter.
- Validation.
- Basic frontend pages.

## Phase 2 - Enrollment and first invoice

Modules:
- Enrollment
- StudentPackage
- Initial Invoice creation

Goal:
- Enroll student into classroom.
- Choose package.
- Snapshot package.
- Create first invoice automatically.

Must have:
- Duplicate active enrollment validation.
- Package belongs to classroom validation.
- Transaction.
- Tests.

## Phase 3 - Invoice, Payment, Debt, Revenue

Modules:
- Invoice
- Payment
- Debt
- Revenue

Goal:
- Record payments correctly.
- Payment is source of truth.
- Debt is calculated correctly.
- Revenue is calculated correctly.

Must have:
- Partial payment.
- Full payment.
- Cancel payment with reason.
- Reject overpayment.
- Recalculate invoice after payment changes.
- Tests.

This is the most important phase for money correctness.

## Phase 4 - Sessions, Attendance, MakeupCredit

Modules:
- ClassSession
- Attendance
- MakeupCredit

Goal:
- Generate class sessions.
- Mark attendance.
- Calculate used sessions.
- Handle excused absence.
- Handle class cancellation.

Must have:
- PRESENT counts as used.
- ABSENT counts as used.
- EXCUSED creates makeup credit.
- Canceled session cannot be marked.
- Tests.

## Phase 5 - Change Package

Modules:
- PackageChangeService
- PackageChangeLog

Goal:
- Change package during active package.
- Close old package.
- Calculate credit/debt.
- Create new package.
- Create new invoice with adjustment.

Must have:
- Transaction.
- Tests for credit and debt.
- PackageChangeLog.
- ActivityLog.

## Phase 6 - Dashboard, Statistics, Reports

Modules:
- Dashboard
- Statistics
- Reports
- ActivityLog display

Goal:
- Admin can understand daily operations quickly.

Dashboard must show:
- Today classes.
- Classes not marked attendance.
- Revenue today.
- Revenue this month.
- Current debt.
- Students near package end.
- Recent payments.

Reports:
- Payment report.
- Debt report.
- Invoice report.
- Attendance report.
- Class session/makeup report.
- Activity log report.

## Phase 7 - Polish, export, backup, testing

Goal:
- Improve usability.
- Add export if needed.
- Add backup scripts if needed.
- Add final test coverage for MVP.

Possible additions:
- Export Excel.
- Print receipt.
- Backup database script.
- UI polish.

Do not add new major modules in this phase.
