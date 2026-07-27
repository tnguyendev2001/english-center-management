import axios from 'axios'
import { apiBaseUrl } from './apiConfig'
import { authStorage } from '../features/auth/authStorage'

export const httpClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.request.use((config) => {
  const token = authStorage.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let handlingUnauthorized = false

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const requestUrl = String(error?.config?.url ?? '')
    const isLoginRequest = requestUrl.includes('/auth/login')

    if (status === 401 && !isLoginRequest) {
      if (!handlingUnauthorized) {
        handlingUnauthorized = true
        authStorage.clear()
        const current = window.location.pathname
        if (current !== '/login') {
          const params = new URLSearchParams()
          params.set('reason', 'expired')
          window.location.assign(`/login?${params.toString()}`)
        }
        handlingUnauthorized = false
      }
    }

    return Promise.reject(error)
  },
)
