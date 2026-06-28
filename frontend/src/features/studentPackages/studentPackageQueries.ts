import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { debtKeys } from '../debts/debtQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { paymentKeys } from '../payments/paymentQueries'
import { reportKeys } from '../reports/reportQueries'
import { studentKeys } from '../students/studentQueries'
import {
  changePackage,
  getClassroomStudentPackages,
  getStudentPackages,
  previewChangePackage,
} from './studentPackageApi'
import type { ChangePackagePayload, ChangePackagePreviewPayload } from './studentPackageTypes'

export const studentPackageKeys = {
  all: ['studentPackages'] as const,
  byStudent: (studentId: number) => ['studentPackages', 'student', studentId] as const,
  byClassroom: (classroomId: number) => ['studentPackages', 'classroom', classroomId] as const,
}

export function useStudentPackages(studentId: number) {
  return useQuery({
    queryKey: studentPackageKeys.byStudent(studentId),
    queryFn: () => getStudentPackages(studentId),
    enabled: Number.isFinite(studentId),
  })
}

export function useClassroomStudentPackages(classroomId: number) {
  return useQuery({
    queryKey: studentPackageKeys.byClassroom(classroomId),
    queryFn: () => getClassroomStudentPackages(classroomId),
    enabled: Number.isFinite(classroomId),
  })
}

export function usePreviewChangePackage(studentPackageId?: number) {
  return useMutation({
    mutationFn: (payload: ChangePackagePreviewPayload) => {
      if (!studentPackageId) {
        throw new Error('Student package is required')
      }

      return previewChangePackage(studentPackageId, payload)
    },
  })
}

export function useChangePackage(studentPackageId?: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ChangePackagePayload) => {
      if (!studentPackageId) {
        throw new Error('Student package is required')
      }

      return changePackage(studentPackageId, payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studentPackageKeys.all })
      queryClient.invalidateQueries({ queryKey: studentKeys.all })
      queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
      queryClient.invalidateQueries({ queryKey: debtKeys.all })
      queryClient.invalidateQueries({ queryKey: paymentKeys.all })
      queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      queryClient.invalidateQueries({ queryKey: reportKeys.all })
    },
  })
}
