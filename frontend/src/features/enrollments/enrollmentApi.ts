import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Enrollment, EnrollmentSearchParams, EnrollStudentPayload } from './enrollmentTypes'

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
