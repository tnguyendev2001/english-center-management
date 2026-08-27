const ACCESS_TOKEN_KEY = 'school_admin_access_token'

type TokenClearedListener = () => void

const tokenClearedListeners = new Set<TokenClearedListener>()

export function getAccessToken(): string | null {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function clearAccessToken(): void {
  const hadToken = sessionStorage.getItem(ACCESS_TOKEN_KEY) !== null
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)

  if (hadToken) {
    tokenClearedListeners.forEach((listener) => listener())
  }
}

export function onAccessTokenCleared(listener: TokenClearedListener): () => void {
  tokenClearedListeners.add(listener)
  return () => tokenClearedListeners.delete(listener)
}
