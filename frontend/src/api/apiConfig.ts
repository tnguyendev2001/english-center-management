const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

function resolveApiBaseUrl(): string {
  if (configuredApiBaseUrl) {
    return configuredApiBaseUrl
  }

  if (import.meta.env.DEV) {
    return 'http://localhost:8080/api'
  }

  throw new Error('VITE_API_BASE_URL is required for production builds')
}

export const apiBaseUrl = resolveApiBaseUrl()
