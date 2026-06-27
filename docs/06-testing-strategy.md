# 06 - Testing Strategy

## 1. Testing principle

This app has important money and session-counting rules.

Do not rely only on manual testing.

Every money-related service must have tests.

## 2. Types of tests

### Unit tests

Use for pure business calculation.

Examples:
- Invoice amount calculation.
- Payment amount validation.
- Package change adjustment calculation.
- Used session calculation.

### Service/integration tests

Use for transactional flows.

Examples:
- Enrollment creates student package and invoice.
- Create payment recalculates invoice.
- Cancel payment recalculates invoice.
- Package change closes old package and creates new package/invoice.

## 3. Must-test money cases

Payment:
- Create partial payment.
- Create full payment.
- Reject zero/negative payment.
- Reject overpayment.
- Reject payment for canceled invoice.
- Reject payment for paid invoice.
- Cancel payment with reason.
- Canceled payment is excluded from paidAmount.
- Canceled payment is excluded from revenue.
- Invoice status updates after payment.
- Invoice status updates after payment cancel.

Invoice:
- Create invoice from enrollment.
- Invoice snapshots package name, total sessions, price.
- Invoice is not affected when original TuitionPackage changes.
- Canceled invoice is not counted as debt.

Debt:
- Unpaid invoice counted as debt.
- Partially paid invoice counted as debt.
- Paid invoice not counted as debt.
- Canceled invoice not counted as debt.
- Debt updates after payment cancel.

Revenue:
- Revenue counts valid payments only.
- Revenue excludes canceled payments.
- Revenue does not count invoice amount directly.

## 4. Must-test attendance/session cases

Attendance:
- PRESENT counts as used session.
- ABSENT counts as used session.
- EXCUSED does not count as used session.
- EXCUSED creates makeup credit.
- Cannot mark attendance for canceled session.
- One student has only one attendance record per session.
- Re-marking attendance updates existing record.

Class session:
- Canceled session does not count for students.
- Canceling session requires reason.
- Canceled session should allow makeup session suggestion.

Makeup:
- EXCUSED creates available makeup credit.
- ABSENT does not create makeup credit.
- Makeup credit can be marked used.
- Canceled makeup credit is not counted.

## 5. Must-test package change cases

Package change:
- Change package when student has credit.
- Change package when student has debt.
- Old package is closed.
- New package is created.
- New invoice has adjustment.
- Old invoice is not edited directly.
- PackageChangeLog is created.
- ActivityLog is created.

Example credit case:
- Old package: 8 sessions / 500,000
- Paid: 500,000
- Used: 3 sessions
- Unit price: 62,500
- Used amount: 187,500
- Credit: 312,500
- New package: 16 sessions / 900,000
- New invoice final amount: 587,500

Example debt case:
- Old package: 8 sessions / 500,000
- Paid: 200,000
- Used: 4 sessions
- Unit price: 62,500
- Used amount: 250,000
- Debt: 50,000
- New package: 12 sessions / 700,000
- New invoice final amount: 750,000

## 6. Cursor rule

Cursor must not generate money-related code without tests.

If implementation touches:
- Invoice
- Payment
- Debt
- Revenue
- Package change
- Attendance session count
- Makeup credit

then tests are required.
