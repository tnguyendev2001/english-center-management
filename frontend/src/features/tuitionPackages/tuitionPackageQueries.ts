import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createTuitionPackage,
  deactivateTuitionPackage,
  getTuitionPackage,
  getTuitionPackages,
  updateTuitionPackage,
} from './tuitionPackageApi'
import type { TuitionPackagePayload, TuitionPackageSearchParams } from './tuitionPackageTypes'

export const tuitionPackageKeys = {
  all: ['tuitionPackages'] as const,
  list: (params: TuitionPackageSearchParams) => ['tuitionPackages', 'list', params] as const,
  detail: (id: number) => ['tuitionPackages', 'detail', id] as const,
}

export function useTuitionPackages(params: TuitionPackageSearchParams) {
  return useQuery({
    queryKey: tuitionPackageKeys.list(params),
    queryFn: () => getTuitionPackages(params),
  })
}

export function useTuitionPackageDetail(id: number) {
  return useQuery({
    queryKey: tuitionPackageKeys.detail(id),
    queryFn: () => getTuitionPackage(id),
    enabled: Number.isFinite(id),
  })
}

export function useCreateTuitionPackage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createTuitionPackage,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tuitionPackageKeys.all })
    },
  })
}

export function useUpdateTuitionPackage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: TuitionPackagePayload }) =>
      updateTuitionPackage(id, payload),
    onSuccess: (tuitionPackage) => {
      queryClient.invalidateQueries({ queryKey: tuitionPackageKeys.all })
      queryClient.invalidateQueries({ queryKey: tuitionPackageKeys.detail(tuitionPackage.id) })
    },
  })
}

export function useDeactivateTuitionPackage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deactivateTuitionPackage,
    onSuccess: (tuitionPackage) => {
      queryClient.invalidateQueries({ queryKey: tuitionPackageKeys.all })
      queryClient.invalidateQueries({ queryKey: tuitionPackageKeys.detail(tuitionPackage.id) })
    },
  })
}
