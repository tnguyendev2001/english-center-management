import { useMutation, useQueryClient } from '@tanstack/react-query'
import { classroomKeys } from '../classrooms/classroomQueries'
import { classSessionKeys } from '../classSessions/classSessionQueries'
import { dashboardKeys } from '../dashboard/dashboardQueries'
import { enrollmentKeys } from '../enrollments/enrollmentQueries'
import { invoiceKeys } from '../invoices/invoiceQueries'
import { studentKeys } from '../students/studentQueries'
import { confirmLegacyStudentImport, previewLegacyStudentImport } from './legacyImportApi'

export function usePreviewLegacyStudentImport() {
  return useMutation({
    mutationFn: ({ file, tuitionPackageId }: { file: File; tuitionPackageId: number }) =>
      previewLegacyStudentImport(file, tuitionPackageId),
  })
}

export function useConfirmLegacyStudentImport() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ file, tuitionPackageId }: { file: File; tuitionPackageId: number }) =>
      confirmLegacyStudentImport(file, tuitionPackageId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: studentKeys.all })
      void queryClient.invalidateQueries({ queryKey: classroomKeys.all })
      void queryClient.invalidateQueries({ queryKey: enrollmentKeys.all })
      void queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      void queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
      void queryClient.invalidateQueries({ queryKey: classSessionKeys.all })
    },
  })
}
