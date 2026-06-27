# Project Context

This project is a small English center management system.

The app is built for real daily operation, not for learning, not for portfolio, and not for demonstrating complex architecture.

## Core Users

* One admin/teacher user.
* No complex role permission in V1.
* The app should be friendly for a teacher who also works as the admin.

## Core Goals

* Simple.
* Accurate.
* Easy to use.
* Fast daily operation.
* Easy to audit when money, attendance, session count, or package data is wrong.

## Main Modules

* Dashboard
* Students
* Classrooms
* Tuition Packages
* Classroom Packages
* Enrollment
* Student Packages
* Class Sessions
* Attendance
* Invoices
* Payments
* Debt
* Revenue
* Makeup Credits
* Package Change
* Statistics
* Reports
* Activity Logs

## Tech Stack

Backend:

* Java 21
* Spring Boot
* Spring Web MVC
* Spring Data JPA
* Spring Validation
* Spring Security simple login
* MySQL
* Flyway

Frontend:

* React
* TypeScript
* Vite
* Ant Design
* React Router
* TanStack Query
* Axios
* dayjs

## Architecture

* Modular monolith.
* Backend and frontend are in the same workspace.
* No microservices.
* No API Gateway.
* No RabbitMQ/Kafka.
* No event-driven architecture.
* No enterprise billing complexity.
* No complex role/permission system in V1.

## Most Important Business Principles

### Money

* Payment is the source of truth for paid amount.
* Do not manually edit paidAmount.
* paidAmount must be calculated from valid payments.
* Canceled payments must not be counted.
* Do not hard delete payments.
* If a payment is wrong, cancel it with a reason.
* Do not allow overpayment in V1.
* Revenue must be calculated from valid payments, not from invoice amount.
* Money-related operations must use BigDecimal.
* Money-related operations must be transactional and tested.

### Tuition Package

* One classroom can have many tuition packages.
* A student chooses one package when enrolling in a classroom.
* Package name, total sessions, and price must be snapshotted when creating a student package or invoice.
* Later changes to the original tuition package must not affect old invoices, enrollments, or student packages.

### Attendance and Sessions

* PRESENT counts as one used session.
* ABSENT counts as one used session.
* EXCUSED does not count as used session and creates makeup credit.
* CANCELED class session does not count for any student.
* Do not mark attendance for canceled sessions.

### Package Change

* Do not edit the old package directly.
* Do not edit the old invoice directly.
* Close the old student package.
* Calculate used sessions.
* Calculate used amount.
* Calculate credit or debt.
* Create a new student package.
* Create a new invoice with adjustment.
* Save package change history.
* Save activity log.

## AI Development Rules

Before implementing any task, Cursor must read:

* PROJECT_CONTEXT.md
* docs/01-business-rules.md
* docs/03-api-convention.md
* docs/04-database-convention.md
* docs/05-frontend-convention.md
* docs/06-testing-strategy.md
* .cursor/rules/

## Development Workflow

Implement the app by phase.

Do not build the whole app in one task.

Recommended phases:

1. Phase 0: Project foundation
2. Phase 1: Students, Tuition Packages, Classrooms
3. Phase 2: Enrollment and first invoice creation
4. Phase 3: Invoice, Payment, Debt, Revenue
5. Phase 4: Class Sessions, Attendance, Makeup Credits
6. Phase 5: Package Change
7. Phase 6: Dashboard, Statistics, Reports
8. Phase 7: Polish, export, backup, testing

## Backend and Frontend Sync

When backend DTO or API changes:

* Update frontend types.
* Update frontend API client.
* Update TanStack Query hooks if needed.
* Update affected pages/components.
* Do not let backend and frontend contracts drift.

## Do Not Add Without Explicit Request

Do not add:

* Microservices
* API Gateway
* RabbitMQ
* Kafka
* Event-driven architecture
* Complex role permission
* Redux
* Student wallet
* Auto refund
* Auto package change
* Enterprise billing workflow
* Too many statuses/enums

## Final Principle

Keep the app simple, accurate, practical, and easy to audit.

The system must help a teacher/admin operate the center every day with minimal effort and maximum data correctness.
