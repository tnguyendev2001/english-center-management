import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { classroomKeys } from '../classrooms/classroomQueries'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { reportKeys } from '../reports/reportQueries'
import { enrollStudent, getEnrollment, getEnrollments } from './enrollmentApi'
import type { EnrollmentSearchParams } from './enrollmentTypes'

export const enrollmentKeys = {
  all: ['enrollments'] as const,
  list: (params: EnrollmentSearchParams) => ['enrollments', 'list', params] as const,
  detail: (id: number) => ['enrollments', 'detail', id] as const,
}

export function useEnrollments(params: EnrollmentSearchParams) {
  return useQuery({
    queryKey: enrollmentKeys.list(params),
    queryFn: () => getEnrollments(params),
  })
}

export function useEnrollmentDetail(id: number) {
  return useQuery({
    queryKey: enrollmentKeys.detail(id),
    queryFn: () => getEnrollment(id),
    enabled: Number.isFinite(id),
  })
}

export function useEnrollStudent() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: enrollStudent,
    onSuccess: (enrollment) => {
      queryClient.invalidateQueries({ queryKey: enrollmentKeys.all })
      queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      queryClient.invalidateQueries({ queryKey: reportKeys.all })
      queryClient.invalidateQueries({ queryKey: classroomKeys.detail(enrollment.classroomId) })
      queryClient.invalidateQueries({
        queryKey: classroomKeys.eligibleStudents(enrollment.classroomId),
      })
    },
  })
}
