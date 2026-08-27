import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelMakeupCredit, getMakeupCredits } from './makeupCreditApi'
import { dashboardKeys } from '../dashboard/dashboardQueries'

export const makeupCreditKeys = {
  all: ['makeupCredits'] as const,
}

export function useMakeupCredits() {
  return useQuery({
    queryKey: makeupCreditKeys.all,
    queryFn: getMakeupCredits,
  })
}

export function useCancelMakeupCredit() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: cancelMakeupCredit,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: makeupCreditKeys.all })
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
    },
  })
}
