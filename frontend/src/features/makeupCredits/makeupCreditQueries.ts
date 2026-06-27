import { useQuery } from '@tanstack/react-query'
import { getMakeupCredits } from './makeupCreditApi'

export const makeupCreditKeys = {
  all: ['makeupCredits'] as const,
}

export function useMakeupCredits() {
  return useQuery({
    queryKey: makeupCreditKeys.all,
    queryFn: getMakeupCredits,
  })
}
