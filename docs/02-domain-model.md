# 02 - Domain Model

This document describes the main domain entities.

Use simple modular monolith design. Do not over-model.

## 1. Student

Represents a learner.

Fields:
- id
- studentCode
- fullName
- dateOfBirth
- phone
- parentName
- parentPhone
- address
- status
- note
- createdAt
- updatedAt

Status:
- ACTIVE
- ON_HOLD
- INACTIVE

Rules:
- studentCode must be unique.
- Do not hard delete student if related data exists.
- Student detail should show enrollments, current package, invoices, payments, attendance, and package change history.

## 2. Classroom

Represents a class.

Use "Classroom" in code, not "Class", because class is a Java keyword.

Fields:
- id
- classCode
- className
- level
- teacherName
- room
- startDate
- expectedEndDate
- daysOfWeek
- startTime
- endTime
- status
- note
- createdAt
- updatedAt

Status:
- PLANNED
- ONGOING
- COMPLETED
- CANCELED

Rules:
- classCode must be unique.
- A classroom can have many tuition packages through ClassPackage.
- A classroom should generate class sessions based on weekly schedule.

## 3. TuitionPackage

Represents a package template.

Fields:
- id
- name
- sessionsPerWeek
- totalSessions
- expectedMonths
- price
- status
- createdAt
- updatedAt

Status:
- ACTIVE
- INACTIVE

Rules:
- It is only a template.
- It must be snapshotted when creating StudentPackage or Invoice.
- Updating TuitionPackage later must not affect old invoices.

## 4. ClassPackage

Represents the relationship between Classroom and TuitionPackage.

Fields:
- id
- classroomId
- tuitionPackageId
- active
- createdAt

Rules:
- Unique by classroomId + tuitionPackageId.
- During enrollment, only packages linked to the classroom can be selected.

## 5. Enrollment

Represents student enrollment in a classroom.

Fields:
- id
- studentId
- classroomId
- startDate
- endDate
- status
- selectedPackageId
- packageNameSnapshot
- totalSessionsSnapshot
- packagePriceSnapshot
- discountAmount
- finalAmount
- note
- createdAt
- updatedAt

Status:
- ACTIVE
- ON_HOLD
- DROPPED
- COMPLETED

Rules:
- Student cannot have duplicate ACTIVE enrollment in same classroom.
- Enrollment creates StudentPackage and first Invoice.

## 6. StudentPackage

Represents a student's selected package/cycle.

Fields:
- id
- studentId
- classroomId
- enrollmentId
- tuitionPackageId
- packageName
- totalSessions
- price
- discountAmount
- adjustmentAmount
- finalAmount
- startDate
- endDate
- status
- cycleNo
- createdAt
- updatedAt

Status:
- ACTIVE
- CLOSED
- CANCELED

Rules:
- Used to track remaining sessions.
- Closed when package is completed or changed.
- Do not edit old StudentPackage during package change.

## 7. ClassSession

Represents one lesson session.

Fields:
- id
- classroomId
- sessionNo
- sessionDate
- startTime
- endTime
- status
- cancelReason
- note
- createdAt
- updatedAt

Status:
- SCHEDULED
- COMPLETED
- CANCELED

Rules:
- Canceled session does not count as used session.
- Attendance cannot be marked for canceled session.

## 8. Attendance

Represents a student's attendance for one session.

Fields:
- id
- sessionId
- studentId
- status
- note
- markedAt
- markedBy

Status:
- PRESENT
- ABSENT
- EXCUSED

Rules:
- Unique by sessionId + studentId.
- PRESENT and ABSENT count as used session.
- EXCUSED does not count and creates MakeupCredit.

## 9. Invoice

Represents tuition fee to collect.

Fields:
- id
- invoiceCode
- studentId
- classroomId
- enrollmentId
- studentPackageId
- packageNameSnapshot
- totalSessionsSnapshot
- amount
- discountAmount
- adjustmentAmount
- finalAmount
- paidAmount
- remainingAmount
- dueDate
- status
- note
- cancelReason
- createdAt
- updatedAt
- canceledAt

Status:
- UNPAID
- PARTIALLY_PAID
- PAID
- CANCELED

Rules:
- paidAmount = sum(valid payments).
- remainingAmount = finalAmount - paidAmount.
- Do not manually edit paidAmount.
- Canceled invoice is not counted as debt.

## 10. Payment

Represents actual money received.

Fields:
- id
- paymentCode
- invoiceId
- studentId
- amount
- paymentDate
- method
- status
- note
- cancelReason
- createdBy
- createdAt
- canceledBy
- canceledAt

Method:
- CASH
- BANK_TRANSFER
- OTHER

Status:
- VALID
- CANCELED

Rules:
- Payment is source of truth.
- Do not hard delete.
- Canceled payment is not counted.

## 11. MakeupCredit

Represents makeup credit.

Fields:
- id
- studentId
- classroomId
- sourceSessionId
- reason
- creditSessions
- usedSessions
- status
- note
- createdAt
- updatedAt

Reason:
- EXCUSED_ABSENCE
- CLASS_CANCELED
- MANUAL_ADJUSTMENT

Status:
- AVAILABLE
- USED
- CANCELED

## 12. PackageChangeLog

Represents package change history.

Fields:
- id
- studentId
- classroomId
- oldStudentPackageId
- newStudentPackageId
- oldPackageName
- newPackageName
- usedSessions
- paidAmount
- usedAmount
- adjustmentAmount
- reason
- changedBy
- changedAt

Rules:
- Must be created when package changes.
- Used for audit and explanation.

## 13. ActivityLog

Represents system activity history.

Fields:
- id
- actionType
- targetType
- targetId
- description
- reason
- createdBy
- createdAt
