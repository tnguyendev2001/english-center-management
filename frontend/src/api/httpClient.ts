import axios from 'axios'
import { apiBaseUrl } from './apiConfig'
import {
  clearAccessToken,
  getAccessToken,
} from '../features/auth/tokenStorage'

export const httpClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
})

function isBackendRequest(requestUrl?: string): boolean {
  if (
    !requestUrl ||
    (!/^[a-z][a-z\d+\-.]*:/i.test(requestUrl) &&
      !requestUrl.startsWith('//'))
  ) {
    return true
  }

  try {
    const backendUrl = new URL(apiBaseUrl, window.location.origin)
    const resolvedRequestUrl = new URL(requestUrl, window.location.origin)
    const backendPath = backendUrl.pathname.replace(/\/+$/, '')

    return (
      resolvedRequestUrl.origin === backendUrl.origin &&
      (resolvedRequestUrl.pathname === backendPath ||
        resolvedRequestUrl.pathname.startsWith(`${backendPath}/`))
    )
  } catch {
    return false
  }
}

function isLoginRequest(requestUrl?: string): boolean {
  const path = requestUrl?.split(/[?#]/, 1)[0].replace(/\/+$/, '')
  return path === '/auth/login' || path?.endsWith('/api/auth/login') === true
}

httpClient.interceptors.request.use((config) => {
  if (!isBackendRequest(config.url) || isLoginRequest(config.url)) {
    return config
  }

  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      isBackendRequest(error.config?.url) &&
      !isLoginRequest(error.config?.url)
    ) {
      clearAccessToken()
    }

    return Promise.reject(error)
  },
)
