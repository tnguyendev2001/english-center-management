import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelClassSession,
  generateClassSessions,
  getClassSessions,
  getTodaySessions,
} from './classSessionApi'
import type {
  CancelClassSessionPayload,
  ClassSessionSearchParams,
} from './classSessionTypes'

export const classSessionKeys = {
  all: ['classSessions'] as const,
  list: (params: ClassSessionSearchParams) => ['classSessions', 'list', params] as const,
  today: ['classSessions', 'today'] as const,
}

export function useClassSessions(params: ClassSessionSearchParams) {
  return useQuery({
    queryKey: classSessionKeys.list(params),
    queryFn: () => getClassSessions(params),
  })
}

export function useTodaySessions() {
  return useQuery({
    queryKey: classSessionKeys.today,
    queryFn: getTodaySessions,
  })
}

export function useGenerateClassSessions() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: generateClassSessions,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: classSessionKeys.all })
    },
  })
}

export function useCancelClassSession() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CancelClassSessionPayload }) =>
      cancelClassSession(id, payload),
    onSuccess: (_session, variables) => {
      queryClient.invalidateQueries({ queryKey: classSessionKeys.all })
      queryClient.invalidateQueries({ queryKey: ['attendance', 'session', variables.id] })
    },
  })
}
