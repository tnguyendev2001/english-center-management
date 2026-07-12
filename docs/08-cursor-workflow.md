# 08 - Cursor Workflow

## 1. Main rule

Do not ask Cursor to build the whole app at once.

Always implement one small task or one module at a time.

## 2. Before prompting Cursor

Check:
1. Which phase is this task in?
2. Which business rules apply?
3. Which backend modules are involved?
4. Which frontend screens are involved?
5. Does this task involve money?
6. Does this task require transaction?
7. Does this task require tests?
8. Does this task require API contract changes?
9. Does this task require frontend type updates?
10. Does this task risk adding complexity?

## 3. Standard Cursor prompt template

Use this format:

```text
You are working on the English Center Management System.

Read these files first:
- PROJECT_CONTEXT.md
- docs/01-business-rules.md
- docs/03-api-convention.md
- .cursor/rules/01-business-rules.mdc
- .cursor/rules/02-backend-rules.mdc
- .cursor/rules/03-frontend-rules.mdc

Task:
[Describe the task]

Business rules:
[List relevant business rules]

Constraints:
- Do not add microservices.
- Do not add API Gateway.
- Do not add RabbitMQ/Kafka.
- Do not add new statuses unless required.
- Do not modify unrelated modules.
- Do not expose entities directly.
- Use DTOs.
- Use BigDecimal for money.
- Use @Transactional for money-related operations.
- Add tests if the task touches money/business calculations.

Expected output:
- Files created/modified.
- Tests added.
- Summary of business rules applied.
```

## 4. Prompt for Phase 0

```text
Read the project structure, docs, and .cursor/rules.

Do not implement the full app yet.

Task:
Set up Phase 0 foundation only.

Backend:
- Create common ApiResponse
- Create PageResponse
- Create GlobalExceptionHandler
- Create BusinessException
- Create NotFoundException
- Configure application.yml
- Configure CORS for frontend localhost
- Configure simple Spring Security placeholder for development
- Ensure Flyway folder exists

Frontend:
- Set up React Router
- Set up Ant Design AppLayout
- Set up TanStack Query provider
- Set up Axios httpClient
- Create placeholder pages:
  Dashboard
  Students
  Classrooms
  Tuition Packages
  Invoices
  Payments
  Reports

Constraints:
- Do not implement business modules yet.
- Do not add microservices.
- Do not add Redux.
- Do not add complex auth.
- Keep code simple and easy to extend.

Output:
- List files created/modified.
- Explain how to run backend and frontend.
```

## 5. Prompt for Student module

```text
Implement Student module.

Business rules:
- Student is a learner in the English center.
- Student status only: ACTIVE, ON_HOLD, INACTIVE.
- Do not hard delete students in V1.
- studentCode must be unique.

Task:
Create backend Student module:
- Student entity
- StudentStatus enum
- StudentRepository
- StudentService
- StudentController
- DTOs for create, update, response, search
- Mapper
- Search by studentCode, fullName, phone
- Pagination
- ApiResponse wrapper

Frontend:
- Student list page
- Student form modal
- Student API client
- Student types
- Student queries

Constraints:
- Do not expose entity directly.
- Use validation on request DTO.
- Keep controller thin.
- Use service for business logic.
- Follow existing package structure.
```

## 6. Prompt for TuitionPackage module

```text
Implement TuitionPackage module.

Business rules:
- TuitionPackage is a template.
- It contains sessionsPerWeek, totalSessions, expectedMonths, and price.
- Later changes to TuitionPackage must not affect old invoices because invoices use snapshots.
- Status only: ACTIVE, INACTIVE.

Task:
Create backend CRUD for TuitionPackage:
- name
- sessionsPerWeek
- totalSessions
- expectedMonths
- price
- status

Frontend:
- Tuition package list page
- Create/edit modal
- API client
- Types
- Queries

Constraints:
- Use BigDecimal for price.
- Use validation.
- Do not add complex pricing logic.
```

## 7. Prompt for Enrollment module

```text
Implement Enrollment flow.

Business rules:
- One classroom can have many tuition packages.
- Student chooses one package when enrolling.
- Package name, total sessions and price must be snapshotted.
- Student cannot have two ACTIVE enrollments in the same classroom.
- After enrollment, create StudentPackage and first Invoice automatically.
- Payment is not created at enrollment.

Task:
Create EnrollmentService.enrollStudent().
It should:
1. Validate student exists.
2. Validate classroom exists.
3. Validate package belongs to classroom.
4. Validate no duplicate active enrollment.
5. Create enrollment.
6. Create student package with snapshot.
7. Create invoice with snapshot and final amount.
8. Save activity log.

Constraints:
- Use @Transactional.
- Do not modify unrelated modules.
- Add tests for duplicate enrollment and invoice creation.
```

## 8. Prompt for Payment module

```text
Implement Payment module.

Business rules:
- Payment is the source of truth.
- Do not allow manual editing of invoice.paidAmount.
- paidAmount = sum(valid payments).
- remainingAmount = finalAmount - paidAmount.
- Canceled payments are not counted.
- Do not hard delete payments.
- Cancel payment must store reason.
- Overpayment is not allowed in V1.

Task:
Create:
- Payment entity
- PaymentStatus
- PaymentMethod
- PaymentRepository
- PaymentService
- PaymentController
- CreatePaymentRequest
- CancelPaymentRequest
- PaymentResponse

APIs:
- POST /api/invoices/{invoiceId}/payments
- POST /api/payments/{paymentId}/cancel
- GET /api/payments

After create/cancel payment:
- Recalculate invoice paidAmount
- Recalculate invoice remainingAmount
- Update invoice status

Constraints:
- Use BigDecimal.
- Use @Transactional.
- Add tests for partial payment, full payment, overpayment rejection, cancel payment.
```

## 9. Prompt for Attendance module

```text
Implement Attendance module.

Business rules:
- PRESENT counts as used session.
- ABSENT counts as used session.
- EXCUSED does not count as used session.
- EXCUSED creates makeup credit.
- Canceled class session cannot be marked attendance.
- One student has only one attendance record per session.

Task:
Create attendance marking API.
Input:
- sessionId
- list of student attendance statuses

The service should:
1. Validate session exists and is not canceled.
2. Validate students belong to classroom.
3. Create or update attendance records.
4. Create makeup credit for EXCUSED if not already created.
5. Update session status if needed.
6. Save activity log.

Constraints:
- Use @Transactional.
- Add tests for PRESENT, ABSENT, EXCUSED, canceled session.
```

## 10. Prompt for Package Change

```text
Implement package change flow.

Business rules:
- Do not edit old package directly.
- Close old student package.
- Calculate used sessions.
- usedAmount = usedSessions * oldPackageUnitPrice.
- paidAmount = sum(valid payments of old package invoices).
- If student has credit, subtract it from new invoice.
- If student has debt, add it to new invoice.
- Create new student package.
- Create new invoice with adjustment.
- Save package change log.

Task:
Create PackageChangeService.changePackage().
Create API:
POST /api/student-packages/{id}/change-package

Request:
- newTuitionPackageId
- reason

Response:
- old package summary
- used sessions
- paid amount
- used amount
- adjustment
- new invoice amount

Constraints:
- Use @Transactional.
- Do not modify old invoice amount directly.
- Add tests for credit and debt cases.
```

## 11. Review checklist after Cursor generates code

Backend:
- Controller is thin.
- Business logic is in service.
- DTOs are used.
- Entities are not exposed.
- BigDecimal is used for money.
- @Transactional is used for money operations.
- No hard delete for payment.
- No manual paidAmount update from request.
- Tests exist for money logic.
- ActivityLog is saved for important actions.

Frontend:
- No Axios call directly in page components.
- Feature API files are used.
- TanStack Query is used.
- Types match backend DTO.
- Vietnamese UI labels are clear.
- Cancel actions use confirmation modal.
- Query invalidation is correct.

Database:
- Flyway migration exists.
- No old migration is modified.
- Table/column names use snake_case.
- Money columns use NUMERIC(19,2).
- Important unique constraints exist.

Business:
- Payment remains source of truth.
- Canceled payment is excluded.
- Debt is correct.
- Revenue is correct.
- Package snapshot is preserved.
- Attendance session count is correct.
- Package change does not rewrite old data.
