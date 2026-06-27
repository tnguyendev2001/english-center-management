import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { Student, StudentPayload, StudentSearchParams } from './studentTypes'

export async function getStudents(params: StudentSearchParams) {
  const response = await httpClient.get<ApiResponse<Student[]>>('/students', {
    params,
  })

  return response.data
}

export async function getStudent(id: number) {
  const response = await httpClient.get<ApiResponse<Student>>(`/students/${id}`)

  return response.data.data
}

export async function createStudent(payload: StudentPayload) {
  const response = await httpClient.post<ApiResponse<Student>>('/students', payload)

  return response.data.data
}

export async function updateStudent(id: number, payload: StudentPayload) {
  const response = await httpClient.put<ApiResponse<Student>>(`/students/${id}`, payload)

  return response.data.data
}
