import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { LegacyImportConfirmResponse, LegacyImportPreviewResponse } from './legacyImportTypes'

export async function previewLegacyStudentImport(file: File, tuitionPackageId: number) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('tuitionPackageId', String(tuitionPackageId))

  const response = await httpClient.post<ApiResponse<LegacyImportPreviewResponse>>(
    '/imports/legacy-students/preview',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )

  return response.data.data
}

export async function confirmLegacyStudentImport(file: File, tuitionPackageId: number) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('tuitionPackageId', String(tuitionPackageId))

  const response = await httpClient.post<ApiResponse<LegacyImportConfirmResponse>>(
    '/imports/legacy-students/confirm',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )

  return response.data.data
}
