import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { classSessionKeys } from '../classSessions/classSessionQueries'
import { makeupCreditKeys } from '../makeupCredits/makeupCreditQueries'
import { studentPackageKeys } from '../studentPackages/studentPackageQueries'
import { getAttendance, markAttendance } from './attendanceApi'

export const attendanceKeys = {
  all: ['attendance'] as const,
  bySession: (sessionId?: number) => ['attendance', 'session', sessionId] as const,
}

export function useAttendance(sessionId?: number) {
  return useQuery({
    queryKey: attendanceKeys.bySession(sessionId),
    queryFn: () => getAttendance(sessionId),
    enabled: Number.isFinite(sessionId),
  })
}

export function useMarkAttendance() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: markAttendance,
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: attendanceKeys.bySession(variables.sessionId) })
      queryClient.invalidateQueries({ queryKey: makeupCreditKeys.all })
      queryClient.invalidateQueries({ queryKey: classSessionKeys.all })
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all })
    },
  })
}
