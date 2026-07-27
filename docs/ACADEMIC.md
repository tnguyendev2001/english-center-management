# Phase 11 — Academic / Learning Module

Academic management for a small English center: lessons, materials, assignments, assessments, scores, teacher comments, and printable progress reports.

## Scope (V1)

Included:
- LessonRecord linked 1:1 to ClassSession
- LearningMaterial file upload/download (local storage)
- Assignment + targets + student submissions + grading
- Assessment + bulk score entry + publish-to-students
- EvaluationPeriod + StudentEvaluation + StudentProgressReport
- Shared `AcademicProgressCalculationService`
- ADMIN / TEACHER / STUDENT pages + A4 print route
- Dashboard alerts (computed, no notification DB)

Not included:
- Live class, chat/forum, timed online exams, question banks, auto-marking, AI grading, plagiarism, parent role, certificates, public rankings

## Ownership

| Role | Access |
|------|--------|
| ADMIN | Full academic access |
| TEACHER | Only classrooms where `Classroom.teacherId` matches linked teacher |
| STUDENT | Own enrolled-class data only; studentId always from JWT |

Helpers: `AcademicAccessService`, `AuthorizationService.canManageLesson/Assignment/Assessment/...`.

## File storage

- Config: `app.storage.base-dir` (default `uploads/academic`), `app.storage.max-file-size-bytes` (20MB)
- Allowed: pdf, doc/docx, xls/xlsx, ppt/pptx, jpg/jpeg/png, txt
- DB stores metadata + relative `storageKey` only — never binary/base64 or absolute paths
- Download checks role + visibility (`TEACHER_ONLY` / `CLASS_STUDENTS`)

## Score rules

- Money-style `BigDecimal` for maxScore / score / weight / averages
- GRADED: `0 <= score <= maxScore`
- ABSENT does not become 0 unless teacher enters 0
- EXEMPT excluded from averages
- ABSENT with explicit score may contribute to average
- Bulk `PUT /api/academic/assessments/{id}/scores` is transactional all-or-nothing
- Official averages computed only on backend

### Formulas

```
normalizedPercentage = score / maxScore * 100   (HALF_UP, display 2 dp)
averageAssessmentPercentage = mean(normalized) for included graded rows
weightedAssessmentAverage = Σ(normalized * weight) / Σ(weight)  or null if no weights
attendanceRate = present / sessionsHeld * 100
assignmentCompletionRate = submitted / assigned * 100
```

## Main APIs

| Area | Base |
|------|------|
| Lessons | `/api/academic/lessons` |
| Materials | `/api/academic/materials` |
| Assignments | `/api/academic/assignments` |
| Submissions | `/api/academic/assignments/{id}/submissions`, `/api/academic/submissions/{id}/grade` |
| Assessments | `/api/academic/assessments` (+ `/{id}/scores`, `/publish-scores`) |
| Periods / Evaluations | `/api/academic/evaluation-periods`, `/api/academic/evaluations` |
| Reports | `/api/academic/progress-reports`, `/{id}/document` |
| Student me | `/api/me/lessons\|materials\|assignments\|assessments\|scores\|evaluations\|progress-reports` |

## Frontend routes

| Role | Routes |
|------|--------|
| ADMIN | `/academic/lessons`, `/assignments`, `/assessments`, `/evaluations`, `/progress-reports` |
| TEACHER | `/me/lessons`, `/me/assignments`, `/me/assessments`, `/me/evaluations`, `/me/academic-reports` |
| STUDENT | `/student/learning` tabs: lessons, materials, assignments, results, feedback |
| Print | `/print/progress-reports/:reportId` |

## Migration

`V28__create_academic_module.sql` — uniqueness on lesson↔session, assignment targets, submissions, scores, evaluations.

## Manual test checklist

See Phase 11 TC01–TC17 in the implementation request (ownership, late submit, bulk score rollback, publish visibility, print layout, mobile score entry).
