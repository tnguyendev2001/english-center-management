import { httpClient } from '../../api/httpClient'
import type { ApiResponse } from '../../api/apiResponse'
import type {
  AuthUser,
  ChangePasswordRequest,
  CreateUserRequest,
  CreateUserResponse,
  LoginRequest,
  LoginResponse,
  ResetPasswordResponse,
  AccountRole,
  AccountStatus,
  UserAccount,
} from './authTypes'

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const response = await httpClient.post<ApiResponse<LoginResponse>>('/auth/login', payload)
  return response.data.data
}

export async function fetchCurrentUser(): Promise<AuthUser> {
  const response = await httpClient.get<ApiResponse<AuthUser>>('/auth/me')
  return response.data.data
}

export async function changePassword(payload: ChangePasswordRequest): Promise<void> {
  await httpClient.post('/auth/change-password', payload)
}

export async function searchUsers(params: {
  username?: string
  role?: AccountRole
  status?: AccountStatus
  page?: number
  size?: number
}): Promise<{ items: UserAccount[]; totalElements: number }> {
  const response = await httpClient.get<ApiResponse<UserAccount[]>>('/admin/users', { params })
  return {
    items: response.data.data,
    totalElements: response.data.meta?.totalElements ?? response.data.data.length,
  }
}

export async function createUser(payload: CreateUserRequest): Promise<CreateUserResponse> {
  const response = await httpClient.post<ApiResponse<CreateUserResponse>>('/admin/users', payload)
  return response.data.data
}

export async function enableUser(id: number): Promise<UserAccount> {
  const response = await httpClient.patch<ApiResponse<UserAccount>>(`/admin/users/${id}/enable`)
  return response.data.data
}

export async function disableUser(id: number): Promise<UserAccount> {
  const response = await httpClient.patch<ApiResponse<UserAccount>>(`/admin/users/${id}/disable`)
  return response.data.data
}

export async function resetUserPassword(id: number): Promise<ResetPasswordResponse> {
  const response = await httpClient.post<ApiResponse<ResetPasswordResponse>>(
    `/admin/users/${id}/reset-password`,
  )
  return response.data.data
}
