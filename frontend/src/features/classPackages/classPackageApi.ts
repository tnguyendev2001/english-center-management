import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { AddClassPackagePayload, ClassPackage } from './classPackageTypes'

export async function getClassPackages(classroomId: number) {
  const response = await httpClient.get<ApiResponse<ClassPackage[]>>(
    `/classrooms/${classroomId}/packages`,
  )

  return response.data.data
}

export async function addClassPackage(classroomId: number, payload: AddClassPackagePayload) {
  const response = await httpClient.post<ApiResponse<ClassPackage>>(
    `/classrooms/${classroomId}/packages`,
    payload,
  )

  return response.data.data
}

export async function deactivateClassPackage(classroomId: number, tuitionPackageId: number) {
  const response = await httpClient.post<ApiResponse<ClassPackage>>(
    `/classrooms/${classroomId}/packages/${tuitionPackageId}/deactivate`,
  )

  return response.data.data
}
