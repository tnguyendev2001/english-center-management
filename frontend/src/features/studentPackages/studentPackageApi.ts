import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  ChangePackagePayload,
  ChangePackagePreviewPayload,
  ChangePackagePreview,
  ChangePackageResult,
  AdjustPackagePeriodStartPayload,
  EnrollmentLearningProgress,
  StudentPackagePeriod,
} from './studentPackageTypes'

export async function getStudentPackages(studentId: number) {
  const response = await httpClient.get<ApiResponse<EnrollmentLearningProgress[]>>(
    `/students/${studentId}/packages`,
  )

  return response.data.data
}

export async function getClassroomStudentPackages(classroomId: number) {
  const response = await httpClient.get<ApiResponse<EnrollmentLearningProgress[]>>(
    `/classrooms/${classroomId}/student-packages`,
  )

  return response.data.data
}

export async function previewChangePackage(
  studentPackageId: number,
  payload: ChangePackagePreviewPayload,
) {
  const response = await httpClient.post<ApiResponse<ChangePackagePreview>>(
    `/student-packages/${studentPackageId}/change-package/preview`,
    payload,
  )

  return response.data.data
}

export async function changePackage(studentPackageId: number, payload: ChangePackagePayload) {
  const response = await httpClient.post<ApiResponse<ChangePackageResult>>(
    `/student-packages/${studentPackageId}/change-package`,
    payload,
  )

  return response.data.data
}

export async function getStudentPackagePeriod(studentPackageId: number) {
  const response = await httpClient.get<ApiResponse<StudentPackagePeriod>>(
    `/student-packages/${studentPackageId}/period`,
  )
  return response.data.data
}

export async function recalculatePackageTimeline(enrollmentId: number) {
  const response = await httpClient.post<ApiResponse<StudentPackagePeriod[]>>(
    `/enrollments/${enrollmentId}/package-timeline/recalculate`,
  )
  return response.data.data
}

export async function adjustStudentPackagePeriodStart(
  studentPackageId: number,
  payload: AdjustPackagePeriodStartPayload,
) {
  const response = await httpClient.patch<ApiResponse<StudentPackagePeriod>>(
    `/student-packages/${studentPackageId}/period-start`,
    payload,
  )
  return response.data.data
}
