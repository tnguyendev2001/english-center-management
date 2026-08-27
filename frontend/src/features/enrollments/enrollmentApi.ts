import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  CancelEnrollmentPayload,
  Enrollment,
  EnrollmentSearchParams,
  EnrollmentStatusHistory,
  EnrollStudentPayload,
  HoldEnrollmentPayload,
  PauseEnrollmentPayload,
  ChangeLearningStartDatePayload,
  ReactivateEnrollmentPayload,
  StopEnrollmentPayload,
  TransferEnrollmentPayload,
  TransferEnrollmentResult,
  EnrollmentLifecycleContext,
} from './enrollmentTypes'

export async function getEnrollments(params: EnrollmentSearchParams) {
  const response = await httpClient.get<ApiResponse<Enrollment[]>>('/enrollments', {
    params,
  })

  return response.data
}

export async function getEnrollment(id: number) {
  const response = await httpClient.get<ApiResponse<Enrollment>>(`/enrollments/${id}`)

  return response.data.data
}

export async function enrollStudent(payload: EnrollStudentPayload) {
  const response = await httpClient.post<ApiResponse<Enrollment>>('/enrollments', payload)

  return response.data.data
}

export async function pauseEnrollment(id: number, payload: PauseEnrollmentPayload) {
  const response = await httpClient.post<ApiResponse<Enrollment>>(
    `/enrollments/${id}/pause`,
    payload,
  )

  return response.data.data
}

export async function changeLearningStartDate(
  id: number,
  payload: ChangeLearningStartDatePayload,
) {
  const response = await httpClient.post<ApiResponse<Enrollment>>(
    `/enrollments/${id}/learning-start-date`,
    payload,
  )

  return response.data.data
}

export async function stopEnrollment(id: number, payload: StopEnrollmentPayload) {
  const response = await httpClient.post<ApiResponse<Enrollment>>(
    `/enrollments/${id}/stop`,
    payload,
  )

  return response.data.data
}

export async function holdEnrollment(id: number, payload: HoldEnrollmentPayload) {
  const response = await httpClient.post<ApiResponse<Enrollment>>(
    `/enrollments/${id}/hold`,
    payload,
  )

  return response.data.data
}

export async function reactivateEnrollment(id: number, payload: ReactivateEnrollmentPayload) {
  const response = await httpClient.post<ApiResponse<Enrollment>>(
    `/enrollments/${id}/reactivate`,
    payload,
  )

  return response.data.data
}

export async function transferEnrollment(id: number, payload: TransferEnrollmentPayload) {
  const response = await httpClient.post<ApiResponse<TransferEnrollmentResult>>(
    `/enrollments/${id}/transfer`,
    payload,
  )

  return response.data.data
}

export async function cancelEnrollment(id: number, payload: CancelEnrollmentPayload) {
  const response = await httpClient.post<ApiResponse<Enrollment>>(
    `/enrollments/${id}/cancel`,
    payload,
  )

  return response.data.data
}

export async function getEnrollmentStatusHistory(id: number) {
  const response = await httpClient.get<ApiResponse<EnrollmentStatusHistory[]>>(
    `/enrollments/${id}/status-history`,
  )

  return response.data.data
}

export async function getEnrollmentLifecycleContext(id: number) {
  const response = await httpClient.get<ApiResponse<EnrollmentLifecycleContext>>(
    `/enrollments/${id}/lifecycle-context`,
  )

  return response.data.data
}
