# 04 - Database Convention

## 1. Database

Use MySQL.

Use Flyway for schema migrations.

Do not use Hibernate `ddl-auto=update`.

Use:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

## 2. Migration folder

```text
backend/src/main/resources/db/migration/
```

Migration file format:

```text
V1__create_students_table.sql
V2__create_tuition_packages_table.sql
V3__create_classrooms_table.sql
```

Rules:
- Do not modify old migration after it has been applied.
- Create a new migration for every schema change.
- Keep migrations simple and readable.

## 3. Table naming

Use snake_case.

Recommended tables:

```text
students
classrooms
tuition_packages
class_packages
enrollments
student_packages
class_sessions
attendance_records
invoices
payments
makeup_credits
package_change_logs
activity_logs
```

## 4. Column naming

Use snake_case.

Examples:

```text
student_code
full_name
parent_phone
created_at
updated_at
canceled_at
cancel_reason
paid_amount
remaining_amount
```

Foreign keys end with `_id`.

Examples:

```text
student_id
classroom_id
invoice_id
payment_id
student_package_id
```

## 5. Money

Java:
```text
BigDecimal
```

MySQL:
```sql
DECIMAL(15,2)
```

Do not use:
- double
- float

Money columns:
- price
- amount
- discount_amount
- adjustment_amount
- final_amount
- paid_amount
- remaining_amount

## 6. Date/time

Java:
- LocalDate for dates.
- LocalTime for times.
- LocalDateTime for timestamps.

MySQL:
- DATE for dates.
- TIME for times.
- DATETIME for timestamps.

Common timestamp columns:
- created_at
- updated_at
- canceled_at
- changed_at
- marked_at

## 7. Important constraints

Recommended unique constraints:

```text
students.student_code unique
classrooms.class_code unique
invoices.invoice_code unique
payments.payment_code unique
attendance_records(session_id, student_id) unique
class_packages(classroom_id, tuition_package_id) unique
```

## 8. Important indexes

Add indexes for frequent filters:

```text
students(full_name)
students(phone)
classrooms(status)
invoices(student_id)
invoices(classroom_id)
invoices(status)
payments(invoice_id)
payments(student_id)
payments(payment_date)
class_sessions(classroom_id)
class_sessions(session_date)
attendance_records(session_id)
attendance_records(student_id)
activity_logs(created_at)
```

## 9. Financial correctness

Do not rely on frontend for money correctness.

Backend service must enforce:
- No overpayment.
- No payment for canceled invoice.
- No payment for paid invoice.
- Canceled payment excluded from paidAmount and revenue.
- paidAmount recalculated after create/cancel payment.
- remainingAmount recalculated after create/cancel payment.
- status recalculated after create/cancel payment.

Payment create/cancel must be transactional.

Package change must be transactional.
