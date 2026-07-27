import type { AuthUser } from './authTypes'

const ACCESS_TOKEN_KEY = 'ecm.accessToken'
const USER_KEY = 'ecm.authUser'

export const authStorage = {
  getAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY)
  },

  setAccessToken(token: string) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
  },

  getUser(): AuthUser | null {
    const raw = sessionStorage.getItem(USER_KEY)
    if (!raw) {
      return null
    }
    try {
      return JSON.parse(raw) as AuthUser
    } catch {
      return null
    }
  },

  setUser(user: AuthUser) {
    sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  },

  clear() {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  },
}
