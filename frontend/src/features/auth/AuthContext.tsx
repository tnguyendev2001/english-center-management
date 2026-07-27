import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { changePassword as changePasswordApi, fetchCurrentUser, login as loginApi } from './authApi'
import { authStorage } from './authStorage'
import type { AuthUser, ChangePasswordRequest, LoginRequest } from './authTypes'

interface AuthContextValue {
  user: AuthUser | null
  accessToken: string | null
  loading: boolean
  isAuthenticated: boolean
  login: (payload: LoginRequest) => Promise<AuthUser>
  logout: () => void
  refreshUser: () => Promise<AuthUser | null>
  changePassword: (payload: ChangePasswordRequest) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(authStorage.getUser())
  const [accessToken, setAccessToken] = useState<string | null>(authStorage.getAccessToken())
  const [loading, setLoading] = useState(true)

  const logout = useCallback(() => {
    authStorage.clear()
    setUser(null)
    setAccessToken(null)
  }, [])

  const refreshUser = useCallback(async () => {
    const token = authStorage.getAccessToken()
    if (!token) {
      setUser(null)
      setAccessToken(null)
      return null
    }
    try {
      const current = await fetchCurrentUser()
      authStorage.setUser(current)
      setUser(current)
      setAccessToken(token)
      return current
    } catch {
      authStorage.clear()
      setUser(null)
      setAccessToken(null)
      return null
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const token = authStorage.getAccessToken()
      if (!token) {
        if (!cancelled) {
          setLoading(false)
        }
        return
      }
      await refreshUser()
      if (!cancelled) {
        setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [refreshUser])

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await loginApi(payload)
    authStorage.setAccessToken(response.accessToken)
    authStorage.setUser(response.user)
    setAccessToken(response.accessToken)
    setUser(response.user)
    return response.user
  }, [])

  const changePassword = useCallback(async (payload: ChangePasswordRequest) => {
    await changePasswordApi(payload)
    logout()
  }, [logout])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken,
      loading,
      isAuthenticated: Boolean(user && accessToken),
      login,
      logout,
      refreshUser,
      changePassword,
    }),
    [user, accessToken, loading, login, logout, refreshUser, changePassword],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
