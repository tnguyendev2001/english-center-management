import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getCenterProfile, updateCenterProfile } from './centerProfileApi'
import type { UpdateCenterProfilePayload } from './centerProfileTypes'

export const centerProfileKeys = {
  all: ['center-profile'] as const,
  detail: () => ['center-profile', 'detail'] as const,
}

export function useCenterProfile(enabled = true) {
  return useQuery({
    queryKey: centerProfileKeys.detail(),
    queryFn: getCenterProfile,
    enabled,
  })
}

export function useUpdateCenterProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: UpdateCenterProfilePayload) => updateCenterProfile(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: centerProfileKeys.all })
    },
  })
}
