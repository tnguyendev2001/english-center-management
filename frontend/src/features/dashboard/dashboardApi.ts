import type { ApiResponse } from '../../api/apiResponse'
import { httpClient } from '../../api/httpClient'
import type {
  DashboardDebtAlert,
  DashboardRecentPayment,
  DashboardSummary,
  DashboardTodaySession,
  SessionWarning,
  SessionWarningParams,
} from './dashboardTypes'

export async function getDashboardSummary() {
  const response = await httpClient.get<ApiResponse<DashboardSummary>>('/dashboard/summary')
  return response.data.data
}

export async function getDashboardTodaySessions() {
  const response = await httpClient.get<ApiResponse<DashboardTodaySession[]>>('/dashboard/today-sessions')
  return response.data.data
}

export async function getDashboardDebtAlerts(limit = 10) {
  const response = await httpClient.get<ApiResponse<DashboardDebtAlert[]>>('/dashboard/debt-alerts', {
    params: { limit },
  })
  return response.data.data
}

export async function getSessionWarnings(params: SessionWarningParams = {}) {
  const response = await httpClient.get<ApiResponse<SessionWarning[]>>('/dashboard/session-warnings', {
    params,
  })
  return response.data.data
}

export async function getDashboardRecentPayments(limit = 10) {
  const response = await httpClient.get<ApiResponse<DashboardRecentPayment[]>>('/dashboard/recent-payments', {
    params: { limit },
  })
  return response.data.data
}
