import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type { MakeupCredit } from './makeupCreditTypes'

export async function getMakeupCredits() {
  const response = await httpClient.get<ApiResponse<MakeupCredit[]>>('/makeup-credits', {
    params: { page: 0, size: 100 },
  })

  return response.data.data
}
