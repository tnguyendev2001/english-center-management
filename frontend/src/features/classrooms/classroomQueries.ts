import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createClassroom, getClassroom, getClassrooms, updateClassroom } from './classroomApi'
import type { ClassroomPayload, ClassroomSearchParams } from './classroomTypes'

export const classroomKeys = {
  all: ['classrooms'] as const,
  list: (params: ClassroomSearchParams) => ['classrooms', 'list', params] as const,
  detail: (id: number) => ['classrooms', 'detail', id] as const,
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
