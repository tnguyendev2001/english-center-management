import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'

export interface LoginCredentials {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface CurrentAdmin {
  username: string
  role: 'ADMIN'
}

export async function login(
  credentials: LoginCredentials,
): Promise<LoginResult> {
  const response = await httpClient.post<ApiResponse<LoginResult>>(
    '/auth/login',
    credentials,
  )
  return response.data.data
}

export async function getCurrentAdmin(): Promise<CurrentAdmin> {
  const response =
    await httpClient.get<ApiResponse<CurrentAdmin>>('/auth/me')
  return response.data.data
}
