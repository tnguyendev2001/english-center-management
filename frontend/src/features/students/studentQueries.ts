import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createStudent, getStudent, getStudents, updateStudent } from './studentApi'
import type { StudentPayload, StudentSearchParams } from './studentTypes'

export const studentKeys = {
  all: ['students'] as const,
  list: (params: StudentSearchParams) => ['students', 'list', params] as const,
  detail: (id: number) => ['students', 'detail', id] as const,
}

export function useStudents(params: StudentSearchParams) {
  return useQuery({
    queryKey: studentKeys.list(params),
    queryFn: () => getStudents(params),
  })
}

export function useStudentDetail(id: number) {
  return useQuery({
    queryKey: studentKeys.detail(id),
    queryFn: () => getStudent(id),
    enabled: Number.isFinite(id),
  })
}

export function useCreateStudent() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createStudent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: studentKeys.all })
    },
  })
}

export function useUpdateStudent() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: StudentPayload }) =>
      updateStudent(id, payload),
    onSuccess: (student) => {
      queryClient.invalidateQueries({ queryKey: studentKeys.all })
      queryClient.invalidateQueries({ queryKey: studentKeys.detail(student.id) })
    },
  })
}
