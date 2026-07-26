import type { Enrollment, EnrollmentStatus } from './enrollmentTypes'

const enrollmentStatusPriority: Record<EnrollmentStatus, number> = {
  ACTIVE: 0,
  ON_HOLD: 1,
  STOPPED: 2,
  TRANSFERRED: 3,
  CANCELED: 4,
}

export function dedupeCurrentEnrollments(enrollments: Enrollment[]) {
  const currentByStudentAndClassroom = new Map<string, Enrollment>()

  enrollments.forEach((enrollment) => {
    const key = `${enrollment.studentId}:${enrollment.classroomId}`
    const current = currentByStudentAndClassroom.get(key)

    if (!current || isPreferredEnrollment(enrollment, current)) {
      currentByStudentAndClassroom.set(key, enrollment)
    }
  })

  return [...currentByStudentAndClassroom.values()]
}

function isPreferredEnrollment(candidate: Enrollment, current: Enrollment) {
  const candidatePriority = enrollmentStatusPriority[candidate.status]
  const currentPriority = enrollmentStatusPriority[current.status]

  return candidatePriority < currentPriority ||
    (candidatePriority === currentPriority && candidate.id > current.id)
}
