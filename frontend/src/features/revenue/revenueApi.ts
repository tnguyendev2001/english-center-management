import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { RevenueSummary } from './revenueTypes'

export async function getRevenueSummary() {
  const response = await httpClient.get<ApiResponse<RevenueSummary>>('/revenue/summary')

  return response.data.data
}
