import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { attendanceKeys } from '../attendance/attendanceQueries'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { debtKeys } from '../debts/debtQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { reportKeys } from '../reports/reportQueries'
import { studentPackageKeys } from '../studentPackages/studentPackageQueries'
import {
  confirmClassroomRenewals,
  createClassroom,
  getClassroom,
  getClassrooms,
  getEligibleStudents,
  getRenewalCandidates,
  previewClassroomRenewals,
  updateClassroom,
} from './classroomApi'
import type { ClassroomPayload, ClassroomRenewalPayload, ClassroomSearchParams } from './classroomTypes'

export const classroomKeys = {
  all: ['classrooms'] as const,
  list: (params: ClassroomSearchParams) => ['classrooms', 'list', params] as const,
  detail: (id: number) => ['classrooms', 'detail', id] as const,
  eligibleStudents: (classroomId: number) => ['classrooms', classroomId, 'eligible-students'] as const,
  renewalCandidates: (classroomId: number, remainingThreshold: number) =>
    ['classrooms', classroomId, 'renewal-candidates', remainingThreshold] as const,
}

export function useClassrooms(params: ClassroomSearchParams) {
  return useQuery({
    queryKey: classroomKeys.list(params),
    queryFn: () => getClassrooms(params),
  })
}

export function useClassroomDetail(id: number) {
  return useQuery({
    queryKey: classroomKeys.detail(id),
    queryFn: () => getClassroom(id),
    enabled: Number.isFinite(id),
  })
}

export function useEligibleStudents(classroomId: number, enabled = true) {
  return useQuery({
    queryKey: classroomKeys.eligibleStudents(classroomId),
    queryFn: () => getEligibleStudents(classroomId),
    enabled: Number.isFinite(classroomId) && enabled,
  })
}

export function useRenewalCandidates(classroomId: number, remainingThreshold: number, enabled = true) {
  return useQuery({
    queryKey: classroomKeys.renewalCandidates(classroomId, remainingThreshold),
    queryFn: () => getRenewalCandidates(classroomId, remainingThreshold),
    enabled: Number.isFinite(classroomId) && enabled,
  })
}

export function usePreviewClassroomRenewals(classroomId: number) {
  return useMutation({
    mutationFn: (payload: ClassroomRenewalPayload) => previewClassroomRenewals(classroomId, payload),
  })
}

export function useConfirmClassroomRenewals(classroomId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ClassroomRenewalPayload) => confirmClassroomRenewals(classroomId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: classroomKeys.all })
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all })
      queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
      queryClient.invalidateQueries({ queryKey: debtKeys.all })
      queryClient.invalidateQueries({ queryKey: attendanceKeys.all })
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      queryClient.invalidateQueries({ queryKey: reportKeys.all })
    },
  })
}

export function useCreateClassroom() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createClassroom,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: classroomKeys.all })
    },
  })
}

export function useUpdateClassroom() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ClassroomPayload }) =>
      updateClassroom(id, payload),
    onSuccess: (classroom) => {
      queryClient.invalidateQueries({ queryKey: classroomKeys.all })
      queryClient.invalidateQueries({ queryKey: classroomKeys.detail(classroom.id) })
    },
  })
}
