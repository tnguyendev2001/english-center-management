import type { EnrollmentLearningProgress } from './studentPackageTypes'

export function buildProgressByEnrollmentId(packages: EnrollmentLearningProgress[]) {
  return new Map(packages.map((progress) => [progress.enrollmentId, progress]))
}

export function buildProgressByStudentId(packages: EnrollmentLearningProgress[]) {
  return new Map(packages.map((progress) => [progress.studentId, progress]))
}
