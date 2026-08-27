import { createContext, useContext } from 'react'
import type { CurrentAdmin, LoginCredentials } from './authApi'

export type AuthStatus =
  | 'loading'
  | 'authenticated'
  | 'unauthenticated'
  | 'error'

export interface AuthContextValue {
  status: AuthStatus
  admin: CurrentAdmin | null
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => void
  retryValidation: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
