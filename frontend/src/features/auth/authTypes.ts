export type AccountRole = 'ADMIN' | 'TEACHER' | 'STUDENT'
export type AccountStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'

export interface LinkedProfileSummary {
  studentId?: number | null
  studentCode?: string | null
  studentName?: string | null
  teacherId?: number | null
  teacherCode?: string | null
  teacherName?: string | null
}

export interface AuthUser {
  id: number
  username: string
  role: AccountRole
  status: AccountStatus
  mustChangePassword: boolean
  linkedProfile?: LinkedProfileSummary | null
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface UserAccount {
  id: number
  username: string
  role: AccountRole
  status: AccountStatus
  studentId?: number | null
  studentCode?: string | null
  studentName?: string | null
  teacherId?: number | null
  teacherCode?: string | null
  teacherName?: string | null
  mustChangePassword: boolean
  lastLoginAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  username: string
  role: AccountRole
  studentId?: number | null
  teacherId?: number | null
}

export interface CreateUserResponse {
  user: UserAccount
  temporaryPassword: string
}

export interface ResetPasswordResponse {
  user: UserAccount
  temporaryPassword: string
}
