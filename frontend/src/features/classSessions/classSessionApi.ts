import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  CancelClassSessionPayload,
  ClassSession,
  ClassSessionSearchParams,
  GenerateClassSessionsPayload,
  GenerateClassSessionsResponse,
} from './classSessionTypes'

export async function getClassSessions(params: ClassSessionSearchParams) {
  const response = await httpClient.get<ApiResponse<ClassSession[]>>('/class-sessions', { params })

  return response.data
}

export async function getTodaySessions() {
  const response = await httpClient.get<ApiResponse<ClassSession[]>>('/dashboard/today-sessions')

  return response.data.data
}

export async function generateClassSessions(payload: GenerateClassSessionsPayload) {
  const response = await httpClient.post<ApiResponse<GenerateClassSessionsResponse>>(
    '/class-sessions/generate',
    payload,
  )

  return response.data.data
}

export async function cancelClassSession(id: number, payload: CancelClassSessionPayload) {
  const response = await httpClient.post<ApiResponse<ClassSession>>(`/class-sessions/${id}/cancel`, payload)

  return response.data.data
}

export async function correctionCancelClassSession(id: number, payload: CancelClassSessionPayload) {
  const response = await httpClient.post<ApiResponse<ClassSession>>(
    `/class-sessions/${id}/correction-cancel`,
    payload,
  )

  return response.data.data
}

export async function restoreClassSession(id: number) {
  const response = await httpClient.post<ApiResponse<ClassSession>>(`/class-sessions/${id}/restore`)

  return response.data.data
}
