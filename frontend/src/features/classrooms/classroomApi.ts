import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Classroom, ClassroomPayload, ClassroomSearchParams } from './classroomTypes'

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

export async function createClassroom(payload: ClassroomPayload) {
  const response = await httpClient.post<ApiResponse<Classroom>>('/classrooms', payload)

  return response.data.data
}

export async function updateClassroom(id: number, payload: ClassroomPayload) {
  const response = await httpClient.put<ApiResponse<Classroom>>(`/classrooms/${id}`, payload)

  return response.data.data
}
