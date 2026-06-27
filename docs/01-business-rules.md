# 01 - Business Rules

This is the most important document for Cursor.

Every implementation must follow these rules.

## 1. Core product rules

- The app is for a small English center.
- The app must stay simple and easy to use.
- The app has one admin/teacher user only in V1.
- Do not add complex workflow unless explicitly requested.
- Do not add many statuses/enums.
- Do not add microservices, API Gateway, RabbitMQ, Kafka, or event-driven architecture.
- All important operations must be easy to audit.

## 2. Tuition package rules

- One classroom can have many tuition packages.
- A student chooses exactly one tuition package when enrolling in a classroom.
- Tuition packages are based on number of sessions, not strict calendar months.
- Example packages:
  - 8 sessions / 500,000
  - 12 sessions / 700,000
  - 16 sessions / 900,000
  - 16 sessions / 1,000,000
- TuitionPackage is only a template.
- When creating a student package or invoice, package information must be snapshotted.
- Snapshot fields include:
  - package name
  - total sessions
  - package price
  - discount amount
  - final amount
- Later changes to TuitionPackage must not affect old enrollments, student packages, or invoices.

## 3. Classroom package rules

- A classroom does not have only one fixed package.
- A classroom has a list of allowed tuition packages.
- During enrollment, the admin chooses one of the classroom's allowed packages for the student.
- Do not allow selecting a package that does not belong to the classroom.

## 4. Enrollment rules

- Enrollment connects a student with a classroom.
- A student cannot have two active enrollments in the same classroom.
- When enrolling:
  1. Validate student exists.
  2. Validate classroom exists.
  3. Validate tuition package belongs to classroom.
  4. Validate no duplicate active enrollment.
  5. Create enrollment.
  6. Create student package with package snapshot.
  7. Create first invoice automatically.
- Payment is not created automatically at enrollment.

## 5. Student package rules

StudentPackage represents the selected package of a student in a classroom.

Statuses should be minimal:
- ACTIVE
- CLOSED
- CANCELED

Rules:
- Active student package is used to calculate remaining sessions.
- Closed student package is used when package is completed or changed.
- Canceled student package is used only when created incorrectly.
- Do not edit old student package directly during package change.

## 6. Invoice rules

Invoice means "tuition fee to collect".

Invoice stores the amount the student should pay.

Invoice status:
- UNPAID
- PARTIALLY_PAID
- PAID
- CANCELED

Invoice amount formula:

finalAmount = amount - discountAmount + adjustmentAmount

Where:
- amount = package price snapshot
- discountAmount = manual discount when creating package/invoice
- adjustmentAmount = package change adjustment
  - negative means credit from old package
  - positive means debt from old package

Paid amount formula:

paidAmount = sum(valid payments of this invoice)

Remaining amount formula:

remainingAmount = finalAmount - paidAmount

Rules:
- Do not manually edit paidAmount.
- Do not manually edit remainingAmount.
- paidAmount and remainingAmount may be stored for faster display, but they must be recalculated from payments after create/cancel payment.
- Do not allow payment for canceled invoice.
- Do not allow payment for fully paid invoice.
- Canceled invoice is not counted as debt.
- Fully paid invoice is not counted as debt.
- If invoice is created incorrectly, cancel it with reason; do not hard delete.

## 7. Payment rules

Payment is the source of truth for money received.

Payment status:
- VALID
- CANCELED

Payment methods:
- CASH
- BANK_TRANSFER
- OTHER

Rules:
- Payment is the source of truth for paid amount.
- Do not manually edit invoice.paidAmount from frontend.
- Do not hard delete payment.
- If a payment is wrong, cancel it with reason.
- Canceled payment is not counted in invoice paidAmount.
- Canceled payment is not counted in revenue.
- In V1, overpayment is not allowed.
- Payment amount must be greater than 0.
- Payment amount must not exceed invoice remainingAmount.
- Payment create/cancel must be transactional.
- After creating payment, recalculate invoice paidAmount, remainingAmount, and status.
- After canceling payment, recalculate invoice paidAmount, remainingAmount, and status.

## 8. Debt rules

Debt means unpaid remaining amount.

Debt formula:

debt = sum(remainingAmount of invoices where status is UNPAID or PARTIALLY_PAID)

Rules:
- Canceled invoices are not counted as debt.
- Paid invoices are not counted as debt.
- Debt is not entered manually.
- Debt must update after payment create/cancel.
- Debt report should show:
  - student
  - classroom
  - package
  - finalAmount
  - paidAmount
  - remainingAmount
  - dueDate
  - status

## 9. Revenue rules

Revenue means actual money collected.

Revenue formula:

revenue = sum(valid payments in selected date range)

Rules:
- Do not calculate revenue from invoice amount.
- Do not count canceled payments.
- Do not count unpaid invoices.
- Revenue reports must be based on Payment.

## 10. Class session rules

ClassSession represents one scheduled lesson of a classroom.

Status:
- SCHEDULED
- COMPLETED
- CANCELED

Rules:
- Class sessions are generated from classroom weekly schedule.
- Do not hard delete sessions that have attendance.
- If a session is canceled, students do not lose a session.
- Canceled session cannot be marked attendance.
- If class session is canceled, system should suggest creating makeup session for the class.

## 11. Attendance rules

Attendance statuses:
- PRESENT
- ABSENT
- EXCUSED

Rules:
- PRESENT counts as one used session.
- ABSENT counts as one used session.
- EXCUSED does not count as used session.
- EXCUSED creates makeup credit.
- CANCELED class session does not count for any student.
- One student has only one attendance record per session.
- If attendance is marked again, update existing record instead of creating duplicate.
- Do not mark attendance for canceled session.

Used session formula:

usedSessions = count(PRESENT) + count(ABSENT)

Remaining session formula:

remainingSessions = totalSessionsSnapshot - usedSessions

## 12. Makeup credit rules

MakeupCredit is used for approved absence or class cancellation.

Reasons:
- EXCUSED_ABSENCE
- CLASS_CANCELED
- MANUAL_ADJUSTMENT

Status:
- AVAILABLE
- USED
- CANCELED

Rules:
- EXCUSED attendance creates makeup credit for the student.
- ABSENT does not create makeup credit.
- PRESENT does not create makeup credit.
- Class canceled should allow makeup session for the class.
- Do not automatically refund money for absence in V1.
- Makeup and money are separate concepts.

## 13. Package change rules

Package change is money-sensitive.

Core principle:

Do not edit old package directly.
Do not edit old invoice directly.
Close old student package, calculate used value, then create a new package and new invoice with adjustment.

Flow:
1. Admin selects active student package.
2. System calculates used sessions.
3. System calculates old package unit price.
4. System calculates used amount.
5. System calculates paid amount from valid payments.
6. System calculates credit/debt.
7. Admin selects new tuition package.
8. System creates new student package.
9. System creates new invoice with adjustment.
10. System closes old student package.
11. System saves PackageChangeLog.
12. System saves ActivityLog.

Formula:

unitPrice = oldPackagePrice / oldPackageTotalSessions

usedAmount = usedSessions * unitPrice

adjustment = paidAmount - usedAmount

If adjustment > 0:
- student has credit
- new invoice adjustmentAmount is negative

If adjustment < 0:
- student has debt
- new invoice adjustmentAmount is positive

New invoice formula:

newFinalAmount = newPackagePrice - discountAmount + adjustmentAmount

Examples:
- Old package 8 sessions / 500,000.
- Used 3 sessions.
- Paid 500,000.
- Unit price = 62,500.
- Used amount = 187,500.
- Credit = 312,500.
- New package 16 sessions / 900,000.
- New invoice = 900,000 - 312,500 = 587,500.

Rules:
- Package change must be transactional.
- Must save reason.
- Must save old/new package information.
- Must not rewrite history.

## 14. Activity log rules

Important actions must be logged:
- CREATE_STUDENT
- UPDATE_STUDENT
- CREATE_CLASSROOM
- UPDATE_CLASSROOM
- ENROLL_STUDENT
- CREATE_INVOICE
- CANCEL_INVOICE
- CREATE_PAYMENT
- CANCEL_PAYMENT
- MARK_ATTENDANCE
- CANCEL_SESSION
- CREATE_MAKEUP_CREDIT
- CHANGE_PACKAGE

Activity log should store:
- action type
- target type
- target id
- description
- reason if any
- created by
- created at

## 15. Automation rules

Should automate:
- Create first invoice after enrollment.
- Generate class sessions after creating classroom.
- Calculate paid amount from payment.
- Calculate remaining amount.
- Update invoice status after payment changes.
- Warn students with debt.
- Warn students near package end.
- Warn classes today not marked attendance.
- Create makeup credit for EXCUSED attendance.
- Suggest makeup session after class session cancellation.
- Save activity logs.

Should not fully automate:
- Create next invoice/package without admin confirmation.
- Cancel invoice.
- Cancel payment.
- Change package.
- Refund money.
- Create class makeup session after cancellation without admin confirmation.
