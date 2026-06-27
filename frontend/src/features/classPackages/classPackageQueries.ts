import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addClassPackage, deactivateClassPackage, getClassPackages } from './classPackageApi'
import type { AddClassPackagePayload } from './classPackageTypes'

export const classPackageKeys = {
  all: ['classPackages'] as const,
  list: (classroomId: number) => ['classPackages', 'list', classroomId] as const,
}

export function useClassPackages(classroomId: number) {
  return useQuery({
    queryKey: classPackageKeys.list(classroomId),
    queryFn: () => getClassPackages(classroomId),
    enabled: Number.isFinite(classroomId),
  })
}

export function useAddClassPackage(classroomId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AddClassPackagePayload) => addClassPackage(classroomId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: classPackageKeys.list(classroomId) })
    },
  })
}

export function useDeactivateClassPackage(classroomId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (tuitionPackageId: number) =>
      deactivateClassPackage(classroomId, tuitionPackageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: classPackageKeys.list(classroomId) })
    },
  })
}
