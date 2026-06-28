import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { StudentPackageProgress } from './studentPackageTypes'

export async function getStudentPackages(studentId: number) {
  const response = await httpClient.get<ApiResponse<StudentPackageProgress[]>>(
    `/students/${studentId}/packages`,
  )

  return response.data.data
}

export async function getClassroomStudentPackages(classroomId: number) {
  const response = await httpClient.get<ApiResponse<StudentPackageProgress[]>>(
    `/classrooms/${classroomId}/student-packages`,
  )

  return response.data.data
}
