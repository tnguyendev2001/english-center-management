import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelClassSession,
  correctionCancelClassSession,
  generateClassSessions,
  getClassSession,
  getClassSessions,
  getTodaySessions,
  restoreClassSession,
} from './classSessionApi'
import type {
  CancelClassSessionPayload,
  ClassSessionSearchParams,
} from './classSessionTypes'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { makeupCreditKeys } from '../makeupCredits/makeupCreditQueries'
import { studentPackageKeys } from '../studentPackages/studentPackageQueries'

export const classSessionKeys = {
  all: ['classSessions'] as const,
  list: (params: ClassSessionSearchParams) => ['classSessions', 'list', params] as const,
  detail: (id?: number) => ['classSessions', 'detail', id] as const,
  today: ['classSessions', 'today'] as const,
}

export function useClassSessions(params: ClassSessionSearchParams, enabled = true) {
  return useQuery({
    queryKey: classSessionKeys.list(params),
    queryFn: () => getClassSessions(params),
    enabled,
  })
}

export function useClassSession(id?: number) {
  return useQuery({
    queryKey: classSessionKeys.detail(id),
    queryFn: () => getClassSession(id!),
    enabled: id != null,
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
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
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
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      queryClient.invalidateQueries({ queryKey: ['attendance', 'session', variables.id] })
    },
  })
}

export function useCorrectionCancelClassSession() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CancelClassSessionPayload }) =>
      correctionCancelClassSession(id, payload),
    onSuccess: (_session, variables) => {
      queryClient.invalidateQueries({ queryKey: classSessionKeys.all })
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      queryClient.invalidateQueries({ queryKey: ['attendance', 'session', variables.id] })
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all })
      queryClient.invalidateQueries({ queryKey: makeupCreditKeys.all })
    },
  })
}

export function useRestoreClassSession() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => restoreClassSession(id),
    onSuccess: (_session, id) => {
      queryClient.invalidateQueries({ queryKey: classSessionKeys.all })
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      queryClient.invalidateQueries({ queryKey: ['attendance', 'session', id] })
    },
  })
}
