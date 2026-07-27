import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { CenterProfile, UpdateCenterProfilePayload } from './centerProfileTypes'

export async function getCenterProfile() {
  const response = await httpClient.get<ApiResponse<CenterProfile>>('/center-profile')
  return response.data.data
}

export async function updateCenterProfile(payload: UpdateCenterProfilePayload) {
  const response = await httpClient.put<ApiResponse<CenterProfile>>('/center-profile', payload)
  return response.data.data
}
