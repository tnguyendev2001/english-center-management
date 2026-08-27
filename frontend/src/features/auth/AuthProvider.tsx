import { useCallback, useEffect, useMemo, useState } from 'react'
import { queryClient } from '../../app/queryClient'
import {
  getCurrentAdmin,
  login as requestLogin,
  type CurrentAdmin,
  type LoginCredentials,
} from './authApi'
import {
  clearAccessToken,
  getAccessToken,
  onAccessTokenCleared,
  setAccessToken,
} from './tokenStorage'
import {
  AuthContext,
  type AuthContextValue,
  type AuthStatus,
} from './AuthContext'

interface AuthProviderProps {
  children: React.ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [status, setStatus] = useState<AuthStatus>(() =>
    getAccessToken() ? 'loading' : 'unauthenticated',
  )
  const [admin, setAdmin] = useState<CurrentAdmin | null>(null)

  const validateStoredToken = useCallback(async () => {
    const token = getAccessToken()
    if (!token) {
      setAdmin(null)
      setStatus('unauthenticated')
      return
    }

    setStatus('loading')
    try {
      const currentAdmin = await getCurrentAdmin()
      if (getAccessToken() === token) {
        setAdmin(currentAdmin)
        setStatus('authenticated')
      }
    } catch {
      if (getAccessToken()) {
        setStatus('error')
      } else {
        setAdmin(null)
        setStatus('unauthenticated')
      }
    }
  }, [])

  useEffect(() => {
    const unsubscribe = onAccessTokenCleared(() => {
      queryClient.clear()
      setAdmin(null)
      setStatus('unauthenticated')
    })

    const validationTimer = window.setTimeout(() => {
      void validateStoredToken()
    }, 0)

    return () => {
      window.clearTimeout(validationTimer)
      unsubscribe()
    }
  }, [validateStoredToken])

  const login = useCallback(async (credentials: LoginCredentials) => {
    const result = await requestLogin(credentials)
    setAccessToken(result.accessToken)
    setAdmin({ username: credentials.username, role: 'ADMIN' })
    setStatus('authenticated')
  }, [])

  const logout = useCallback(() => {
    clearAccessToken()
  }, [])

  const retryValidation = useCallback(() => {
    void validateStoredToken()
  }, [validateStoredToken])

  const value = useMemo<AuthContextValue>(
    () => ({ status, admin, login, logout, retryValidation }),
    [status, admin, login, logout, retryValidation],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
