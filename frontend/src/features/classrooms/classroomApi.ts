import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Student } from '../students/studentTypes'
import type {
  Classroom,
  ClassroomPayload,
  ClassroomRenewalCandidate,
  ClassroomRenewalConfirmResult,
  ClassroomRenewalPayload,
  ClassroomRenewalPreview,
  ClassroomSearchParams,
} from './classroomTypes'

export async function getClassrooms(params: ClassroomSearchParams) {
  const response = await httpClient.get<ApiResponse<Classroom[]>>('/classrooms', {
    params,
  })

  return response.data
}

export async function getClassroom(id: number) {
  const response = await httpClient.get<ApiResponse<Classroom>>(`/classrooms/${id}`)

  return response.data.data
}

export async function getEligibleStudents(classroomId: number) {
  const response = await httpClient.get<ApiResponse<Student[]>>(
    `/classrooms/${classroomId}/eligible-students`,
  )

  return response.data.data
}

export async function getRenewalCandidates(classroomId: number, remainingThreshold: number) {
  const response = await httpClient.get<ApiResponse<ClassroomRenewalCandidate[]>>(
    `/classrooms/${classroomId}/renewal-candidates`,
    {
      params: { remainingThreshold },
    },
  )

  return response.data.data
}

export async function previewClassroomRenewals(
  classroomId: number,
  payload: ClassroomRenewalPayload,
) {
  const response = await httpClient.post<ApiResponse<ClassroomRenewalPreview>>(
    `/classrooms/${classroomId}/renewals/preview`,
    payload,
  )

  return response.data.data
}

export async function confirmClassroomRenewals(
  classroomId: number,
  payload: ClassroomRenewalPayload,
) {
  const response = await httpClient.post<ApiResponse<ClassroomRenewalConfirmResult>>(
    `/classrooms/${classroomId}/renewals/confirm`,
    payload,
  )

  return response.data.data
}

export async function createClassroom(payload: ClassroomPayload) {
  const response = await httpClient.post<ApiResponse<Classroom>>('/classrooms', payload)

  return response.data.data
}

export async function updateClassroom(id: number, payload: ClassroomPayload) {
  const response = await httpClient.put<ApiResponse<Classroom>>(`/classrooms/${id}`, payload)

  return response.data.data
}
