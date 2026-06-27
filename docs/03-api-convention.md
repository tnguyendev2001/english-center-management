# 03 - API Convention

## 1. API style

Use REST-style APIs.

Base path:

```text
/api
```

Use JSON request/response.

Do not expose JPA entities directly.

Always use DTO request/response.

## 2. Common response

Success response:

```json
{
  "success": true,
  "message": "Success",
  "data": {}
}
```

List response with pagination:

```json
{
  "success": true,
  "message": "Success",
  "data": [],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

Error response:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "amount",
      "message": "Amount must be greater than 0"
    }
  ]
}
```

## 3. Naming

Controller:
- StudentController
- ClassroomController
- TuitionPackageController
- EnrollmentController
- InvoiceController
- PaymentController

Request DTO:
- CreateStudentRequest
- UpdateStudentRequest
- CreatePaymentRequest
- CancelPaymentRequest

Response DTO:
- StudentResponse
- InvoiceResponse
- PaymentResponse

## 4. Status format

Backend enum values should use English uppercase:

- ACTIVE
- ON_HOLD
- INACTIVE
- UNPAID
- PARTIALLY_PAID
- PAID
- CANCELED
- PRESENT
- ABSENT
- EXCUSED

Frontend can display Vietnamese labels.

Do not invent new statuses without updating:
- docs/01-business-rules.md
- backend enum
- frontend StatusTag
- reports/dashboard if needed

## 5. Date/time format

Use ISO format.

Examples:
- LocalDate: `2026-06-26`
- LocalTime: `18:00:00`
- LocalDateTime: `2026-06-26T18:30:00`

Frontend should use dayjs for display.

## 6. Money format

Backend:
- Use BigDecimal.

Database:
- Use DECIMAL(15,2).

Frontend:
- Receive money as number or string consistently.
- Display with thousand separators.
- Do not do critical money calculations in frontend.
- Backend is source of truth for money calculations.

## 7. Suggested APIs

Students:
```text
GET    /api/students
POST   /api/students
GET    /api/students/{id}
PUT    /api/students/{id}
```

Classrooms:
```text
GET    /api/classrooms
POST   /api/classrooms
GET    /api/classrooms/{id}
PUT    /api/classrooms/{id}
POST   /api/classrooms/{id}/packages
```

Tuition Packages:
```text
GET    /api/tuition-packages
POST   /api/tuition-packages
GET    /api/tuition-packages/{id}
PUT    /api/tuition-packages/{id}
POST   /api/tuition-packages/{id}/deactivate
```

Enrollment:
```text
POST   /api/enrollments
GET    /api/enrollments
GET    /api/enrollments/{id}
```

Invoices:
```text
GET    /api/invoices
GET    /api/invoices/{id}
POST   /api/invoices/{id}/cancel
```

Payments:
```text
GET    /api/payments
POST   /api/invoices/{invoiceId}/payments
POST   /api/payments/{paymentId}/cancel
```

Class sessions:
```text
GET    /api/class-sessions
POST   /api/class-sessions/generate
POST   /api/class-sessions/{id}/cancel
```

Attendance:
```text
POST   /api/attendance/mark
GET    /api/attendance
```

Package change:
```text
POST   /api/student-packages/{id}/change-package
```

Dashboard:
```text
GET    /api/dashboard/summary
GET    /api/dashboard/today-sessions
GET    /api/dashboard/debt-students
GET    /api/dashboard/ending-packages
GET    /api/dashboard/recent-payments
```

Reports:
```text
GET    /api/reports/payments
GET    /api/reports/debts
GET    /api/reports/invoices
GET    /api/reports/attendance
GET    /api/reports/sessions
GET    /api/reports/makeup-credits
GET    /api/reports/activity-logs
```

## 8. API rule for Cursor

When implementing backend API:
- Create request DTO.
- Create response DTO.
- Validate request.
- Use service.
- Do not expose entity.
- Add tests if money or business rules are involved.
- Update frontend types and API client if implementing frontend in same task.
