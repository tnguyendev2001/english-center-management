import { useQuery } from '@tanstack/react-query'
import { getRevenueSummary } from './revenueApi'

export const revenueKeys = {
  all: ['revenue'] as const,
  summary: ['revenue', 'summary'] as const,
}

export function useRevenueSummary() {
  return useQuery({
    queryKey: revenueKeys.summary,
    queryFn: getRevenueSummary,
  })
}
