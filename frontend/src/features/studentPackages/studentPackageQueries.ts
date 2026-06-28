import { useQuery } from '@tanstack/react-query'
import { getClassroomStudentPackages, getStudentPackages } from './studentPackageApi'

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
