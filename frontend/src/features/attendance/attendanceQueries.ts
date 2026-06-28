import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { classSessionKeys } from '../classSessions/classSessionQueries'
import { makeupCreditKeys } from '../makeupCredits/makeupCreditQueries'
import { studentPackageKeys } from '../studentPackages/studentPackageQueries'
import { checkAttendanceReadiness, getAttendance, markAttendance } from './attendanceApi'

export const attendanceKeys = {
  all: ['attendance'] as const,
  bySession: (sessionId?: number) => ['attendance', 'session', sessionId] as const,
  readiness: (sessionId?: number) => ['attendance', 'readiness', sessionId] as const,
}

export function useAttendance(sessionId?: number) {
  return useQuery({
    queryKey: attendanceKeys.bySession(sessionId),
    queryFn: () => getAttendance(sessionId),
    enabled: Number.isFinite(sessionId),
  })
}

export function useAttendanceReadiness(sessionId?: number, enabled = true) {
  const queryClient = useQueryClient()

  return useQuery({
    queryKey: attendanceKeys.readiness(sessionId),
    queryFn: async () => {
      if (!sessionId) {
        throw new Error('Buổi học là bắt buộc')
      }

      const readiness = await checkAttendanceReadiness(sessionId)
      if (readiness.activatedPackages.length > 0) {
        queryClient.invalidateQueries({ queryKey: studentPackageKeys.all })
      }
      return readiness
    },
    enabled: Number.isFinite(sessionId) && enabled,
  })
}

export function useMarkAttendance() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: markAttendance,
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: attendanceKeys.bySession(variables.sessionId) })
      queryClient.invalidateQueries({ queryKey: attendanceKeys.readiness(variables.sessionId) })
      queryClient.invalidateQueries({ queryKey: makeupCreditKeys.all })
      queryClient.invalidateQueries({ queryKey: classSessionKeys.all })
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all })
    },
  })
}
