# 00 - Product Goal

## Product purpose

This application is a small English center management system for real daily operation.

It is not built for:
- Learning technical concepts.
- Portfolio showcase.
- Enterprise architecture demonstration.
- Complex SaaS/platform design.

It is built for:
- A small English center.
- A teacher/admin who manages daily operations.
- Simple and accurate management of students, classes, tuition, payments, debt, attendance, makeup sessions, and reports.

## Main user

V1 has only one user type:

- Admin / Teacher manager

The user can:
- Manage students.
- Manage classrooms.
- Manage tuition packages.
- Enroll students.
- Generate invoices.
- Record payments.
- Track debt.
- Track revenue.
- Create and view class sessions.
- Mark attendance.
- Handle excused absence.
- Handle class cancellation.
- Handle makeup credits.
- Change student tuition packages.
- View dashboard, statistics, and reports.

Do not add complex roles in V1.

## Key daily questions the app must answer

The app should help the admin answer:

- What classes are happening today?
- Which classes have not been marked for attendance?
- Which students still owe tuition?
- Which students are close to finishing their package?
- How much revenue was collected today?
- How much revenue was collected this month?
- How much debt is outstanding?
- What payments were recorded recently?
- Which payments were canceled?
- Why was a payment canceled?
- How many sessions has a student used?
- How many sessions remain?
- Does the student have makeup credits?
- What happened when the student changed package?
- Who changed what and when?

## Product principles

1. Simple before powerful.
2. Accuracy before convenience.
3. Few statuses before many statuses.
4. Auditability before editing old data.
5. Manual confirmation for money-sensitive operations.
6. Automatic calculation for repetitive and error-prone calculations.
7. No hard delete for important financial data.

## Things not included in V1

Do not implement in V1:
- Multiple roles and complex permissions.
- Parent/student portal.
- Mobile app.
- CRM/marketing.
- Teacher salary.
- Multiple branches.
- Student wallet.
- Auto refund.
- Auto package change.
- Microservices.
- API Gateway.
- RabbitMQ.
- Kafka.
- Event-driven architecture.
- Complex accounting system.
