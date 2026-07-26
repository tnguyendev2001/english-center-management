import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import { attendanceKeys } from '../attendance/attendanceQueries'
import { classroomKeys } from '../classrooms/classroomQueries'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { debtKeys } from '../debts/debtQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { reportKeys } from '../reports/reportQueries'
import { studentPackageKeys } from '../studentPackages/studentPackageQueries'
import { studentKeys } from '../students/studentQueries'
import {
  cancelEnrollment,
  enrollStudent,
  getEnrollment,
  getEnrollmentStatusHistory,
  getEnrollments,
  holdEnrollment,
  reactivateEnrollment,
  stopEnrollment,
  transferEnrollment,
} from './enrollmentApi'
import type {
  CancelEnrollmentPayload,
  EnrollmentSearchParams,
  HoldEnrollmentPayload,
  ReactivateEnrollmentPayload,
  StopEnrollmentPayload,
  TransferEnrollmentPayload,
} from './enrollmentTypes'

export const enrollmentKeys = {
  all: ['enrollments'] as const,
  list: (params: EnrollmentSearchParams) => ['enrollments', 'list', params] as const,
  detail: (id: number) => ['enrollments', 'detail', id] as const,
  statusHistory: (id: number) => ['enrollments', 'status-history', id] as const,
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

export function useEnrollmentStatusHistory(id?: number, enabled = true) {
  return useQuery({
    queryKey: enrollmentKeys.statusHistory(id ?? 0),
    queryFn: () => getEnrollmentStatusHistory(id as number),
    enabled: enabled && id != null && Number.isFinite(id),
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

function invalidateEnrollmentLifecycleQueries(queryClient: QueryClient, includeFinancials = false) {
  void queryClient.invalidateQueries({ queryKey: enrollmentKeys.all })
  void queryClient.invalidateQueries({ queryKey: classroomKeys.all })
  void queryClient.invalidateQueries({ queryKey: studentKeys.all })
  void queryClient.invalidateQueries({ queryKey: studentPackageKeys.all })
  void queryClient.invalidateQueries({ queryKey: attendanceKeys.all })
  void queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
  void queryClient.invalidateQueries({ queryKey: reportKeys.all })

  if (includeFinancials) {
    void queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
    void queryClient.invalidateQueries({ queryKey: debtKeys.all })
  }
}

export function useStopEnrollment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: StopEnrollmentPayload }) =>
      stopEnrollment(id, payload),
    onSuccess: () => invalidateEnrollmentLifecycleQueries(queryClient),
  })
}

export function useHoldEnrollment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: HoldEnrollmentPayload }) =>
      holdEnrollment(id, payload),
    onSuccess: () => invalidateEnrollmentLifecycleQueries(queryClient),
  })
}

export function useReactivateEnrollment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ReactivateEnrollmentPayload }) =>
      reactivateEnrollment(id, payload),
    onSuccess: () => invalidateEnrollmentLifecycleQueries(queryClient),
  })
}

export function useTransferEnrollment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: TransferEnrollmentPayload }) =>
      transferEnrollment(id, payload),
    onSuccess: () => invalidateEnrollmentLifecycleQueries(queryClient),
  })
}

export function useCancelEnrollment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CancelEnrollmentPayload }) =>
      cancelEnrollment(id, payload),
    onSuccess: () => invalidateEnrollmentLifecycleQueries(queryClient, true),
  })
}
