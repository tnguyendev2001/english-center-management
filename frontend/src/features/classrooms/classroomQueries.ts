import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { attendanceKeys } from '../attendance/attendanceQueries'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { debtKeys } from '../debts/debtQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { reportKeys } from '../reports/reportQueries'
import { studentPackageKeys } from '../studentPackages/studentPackageQueries'
import {
  assignClassroomTeacher,
  confirmClassroomRenewals,
  createClassroom,
  getActiveTeachers,
  getClassroom,
  getClassrooms,
  getEligibleStudents,
  getRenewalCandidates,
  getUnassignedClassrooms,
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

export function useActiveTeachers(enabled = true) {
  return useQuery({
    queryKey: ['teachers', 'active'],
    queryFn: getActiveTeachers,
    enabled,
  })
}

export function useUnassignedClassrooms(enabled = true) {
  return useQuery({
    queryKey: ['classrooms', 'unassigned'],
    queryFn: getUnassignedClassrooms,
    enabled,
  })
}

function invalidateTeacherRelated(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: classroomKeys.all })
  queryClient.invalidateQueries({ queryKey: ['teachers'] })
  queryClient.invalidateQueries({ queryKey: ['me'] })
  queryClient.invalidateQueries({ queryKey: ['me-classrooms'] })
  queryClient.invalidateQueries({ queryKey: ['me-teacher-dashboard'] })
  queryClient.invalidateQueries({ queryKey: ['me-students'] })
  queryClient.invalidateQueries({ queryKey: ['me-sessions'] })
  queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
}

export function useCreateClassroom() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createClassroom,
    onSuccess: () => {
      invalidateTeacherRelated(queryClient)
    },
  })
}

export function useUpdateClassroom() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ClassroomPayload }) =>
      updateClassroom(id, payload),
    onSuccess: (classroom) => {
      invalidateTeacherRelated(queryClient)
      queryClient.invalidateQueries({ queryKey: classroomKeys.detail(classroom.id) })
    },
  })
}

export function useAssignClassroomTeacher() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, teacherId }: { id: number; teacherId: number }) =>
      assignClassroomTeacher(id, teacherId),
    onSuccess: (classroom) => {
      invalidateTeacherRelated(queryClient)
      queryClient.invalidateQueries({ queryKey: classroomKeys.detail(classroom.id) })
    },
  })
}
