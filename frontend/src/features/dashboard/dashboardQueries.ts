import { useQuery } from '@tanstack/react-query'
import {
  getDashboardDebtAlerts,
  getDashboardOverview,
  getDashboardRecentPayments,
  getDashboardSummary,
  getDashboardTodaySessions,
  getSessionWarnings,
} from './dashboardApi'
import type { SessionWarningParams } from './dashboardTypes'

export const dashboardKeys = {
  all: ['dashboard'] as const,
  overview: ['dashboard', 'overview'] as const,
  summary: ['dashboard', 'summary'] as const,
  todaySessions: ['dashboard', 'today-sessions'] as const,
  debtAlerts: (limit: number) => ['dashboard', 'debt-alerts', limit] as const,
  sessionWarnings: (params: SessionWarningParams) => ['dashboard', 'session-warnings', params] as const,
  recentPayments: (limit: number) => ['dashboard', 'recent-payments', limit] as const,
}

export function useDashboardOverview() {
  return useQuery({
    queryKey: dashboardKeys.overview,
    queryFn: getDashboardOverview,
  })
}

export function useDashboardSummary() {
  return useQuery({
    queryKey: dashboardKeys.summary,
    queryFn: getDashboardSummary,
  })
}

export function useDashboardTodaySessions() {
  return useQuery({
    queryKey: dashboardKeys.todaySessions,
    queryFn: getDashboardTodaySessions,
  })
}

export function useDashboardDebtAlerts(limit = 10) {
  return useQuery({
    queryKey: dashboardKeys.debtAlerts(limit),
    queryFn: () => getDashboardDebtAlerts(limit),
  })
}

export function useSessionWarnings(params: SessionWarningParams = { remainingThreshold: 2 }, enabled = true) {
  return useQuery({
    queryKey: dashboardKeys.sessionWarnings(params),
    queryFn: () => getSessionWarnings(params),
    enabled,
  })
}

export function useDashboardRecentPayments(limit = 10) {
  return useQuery({
    queryKey: dashboardKeys.recentPayments(limit),
    queryFn: () => getDashboardRecentPayments(limit),
  })
}
