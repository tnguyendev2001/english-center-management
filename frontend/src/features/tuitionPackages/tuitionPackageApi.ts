import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  TuitionPackage,
  TuitionPackagePayload,
  TuitionPackageSearchParams,
} from './tuitionPackageTypes'

export async function getTuitionPackages(params: TuitionPackageSearchParams) {
  const response = await httpClient.get<ApiResponse<TuitionPackage[]>>('/tuition-packages', {
    params,
  })

  return response.data
}

export async function getTuitionPackage(id: number) {
  const response = await httpClient.get<ApiResponse<TuitionPackage>>(`/tuition-packages/${id}`)

  return response.data.data
}

export async function createTuitionPackage(payload: TuitionPackagePayload) {
  const response = await httpClient.post<ApiResponse<TuitionPackage>>('/tuition-packages', payload)

  return response.data.data
}

export async function updateTuitionPackage(id: number, payload: TuitionPackagePayload) {
  const response = await httpClient.put<ApiResponse<TuitionPackage>>(
    `/tuition-packages/${id}`,
    payload,
  )

  return response.data.data
}

export async function deactivateTuitionPackage(id: number) {
  const response = await httpClient.post<ApiResponse<TuitionPackage>>(
    `/tuition-packages/${id}/deactivate`,
  )

  return response.data.data
}
